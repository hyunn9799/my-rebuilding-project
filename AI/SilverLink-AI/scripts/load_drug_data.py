"""
e약은요 API 데이터 → MySQL + ChromaDB 적재 스크립트

Usage:
    python -m scripts.load_drug_data
    python -m scripts.load_drug_data --max-pages 5    # 테스트용 (5페이지만)
    python -m scripts.load_drug_data --resume          # 중단 후 재개
"""
import asyncio
import argparse
import json
import os
import sys
import re

# 프로젝트 루트를 sys.path에 추가
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from dotenv import load_dotenv
load_dotenv()

from loguru import logger
from app.integration.drug_api.drug_api_client import DrugApiClient
from app.ocr.repository.drug_repository import DrugRepository
from app.ocr.repository.drug_vector_repository import DrugVectorRepository
from app.ocr.model.drug_model import DrugInfo
from app.chatbot.services.embedding_service import EmbeddingService

# 진행 상태 저장 파일
PROGRESS_FILE = "scripts/.load_progress.json"


def normalize_drug_name(name: str) -> str:
    """약품명 정규화 (인덱스용)"""
    # 괄호 안 내용 제거
    name = re.sub(r"\([^)]*\)", "", name)
    # 특수문자 제거 (한글, 영문, 숫자, 공백만 유지)
    name = re.sub(r"[^\w가-힣a-zA-Z0-9\s]", "", name)
    # 다중 공백 → 단일 공백
    name = re.sub(r"\s+", " ", name).strip()
    return name


def parse_api_item(item: dict) -> DrugInfo:
    """e약은요 API 응답 아이템 → DrugInfo 변환"""
    item_name = item.get("itemName", "")
    return DrugInfo(
        item_seq=str(item.get("itemSeq", "")),
        item_name=item_name,
        item_name_normalized=normalize_drug_name(item_name),
        entp_name=item.get("entpName"),
        efcy_qesitm=item.get("efcyQesitm"),
        use_method_qesitm=item.get("useMethodQesitm"),
        atpn_qesitm=item.get("atpnQesitm"),
        intrc_qesitm=item.get("intrcQesitm"),
        se_qesitm=item.get("seQesitm"),
        deposit_method_qesitm=item.get("depositMethodQesitm"),
        item_image=item.get("itemImage"),
    )


def save_progress(page_no: int, total_loaded: int):
    """진행 상태 저장"""
    os.makedirs(os.path.dirname(PROGRESS_FILE), exist_ok=True)
    with open(PROGRESS_FILE, "w") as f:
        json.dump({"last_page": page_no, "total_loaded": total_loaded}, f)


def load_progress() -> dict:
    """진행 상태 불러오기"""
    if os.path.exists(PROGRESS_FILE):
        with open(PROGRESS_FILE, "r") as f:
            return json.load(f)
    return {"last_page": 0, "total_loaded": 0}


async def main(max_pages: int = None, resume: bool = False, batch_size: int = 100):
    """메인 적재 로직"""
    logger.info("=" * 60)
    logger.info("e약은요 데이터 적재 시작")
    logger.info("=" * 60)

    # 초기화
    api_client = DrugApiClient()
    drug_repo = DrugRepository()
    vector_repo = DrugVectorRepository()
    embedding_service = EmbeddingService()

    # 테이블 생성
    drug_repo.create_table_if_not_exists()

    # 진행 상태 로드
    progress = load_progress() if resume else {"last_page": 0, "total_loaded": 0}
    start_page = progress["last_page"] + 1
    total_loaded = progress["total_loaded"]

    logger.info(f"시작 페이지: {start_page}, 기존 적재: {total_loaded}건")

    page_no = start_page
    consecutive_empty = 0

    while True:
        logger.info(f"--- 페이지 {page_no} 수집 중 ---")

        # API 호출
        data = await api_client.search_by_name(
            item_name="",  # 빈 문자열 → 전체 조회
            page_no=page_no,
            num_of_rows=batch_size,
        )

        # 빈 문자열 조회가 안 되면 itemName 파라미터 없이 재시도
        items = api_client._extract_items(data)
        if not items:
            # 전체 목록 조회 시 다른 방식 시도
            import httpx
            params = {
                "serviceKey": api_client.service_key,
                "pageNo": str(page_no),
                "numOfRows": str(batch_size),
                "type": "json",
            }
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(api_client.BASE_URL, params=params)
                if response.status_code == 200:
                    content_type = response.headers.get("content-type", "")
                    if "json" in content_type:
                        data = response.json()
                        items = api_client._extract_items(data)

        if not items:
            consecutive_empty += 1
            if consecutive_empty >= 3:
                logger.info("3개 연속 빈 페이지, 적재 완료")
                break
            page_no += 1
            continue

        consecutive_empty = 0

        # DrugInfo 변환
        drugs = [parse_api_item(item) for item in items]

        # MySQL 적재
        mysql_count = drug_repo.bulk_upsert(drugs)
        logger.info(f"MySQL 적재: {mysql_count}/{len(drugs)}건")

        # 임베딩 생성 + ChromaDB 적재
        try:
            names = [d.item_name for d in drugs]
            embeddings = embedding_service.create_embeddings(names)
            item_seqs = [d.item_seq for d in drugs]
            metadatas = [
                {"item_name": d.item_name, "entp_name": d.entp_name or ""}
                for d in drugs
            ]
            vector_count = vector_repo.bulk_upsert(item_seqs, embeddings, metadatas)
            logger.info(f"ChromaDB 적재: {vector_count}/{len(drugs)}건")
        except Exception as e:
            logger.error(f"임베딩/ChromaDB 적재 실패: {e}")

        total_loaded += len(drugs)
        save_progress(page_no, total_loaded)
        logger.info(f"누적 적재: {total_loaded}건 (페이지 {page_no})")

        # 페이지 제한 체크
        if max_pages and page_no >= start_page + max_pages - 1:
            logger.info(f"최대 페이지({max_pages}) 도달, 종료")
            break

        # 총 건수 확인
        total_count = api_client._extract_total_count(data)
        if total_count and total_loaded >= total_count:
            logger.info(f"전체 데이터({total_count}건) 적재 완료")
            break

        page_no += 1

    logger.info("=" * 60)
    logger.info(f"적재 완료: 총 {total_loaded}건")
    logger.info(f"MySQL: medications_master 테이블")
    logger.info(f"ChromaDB: {vector_repo.get_count()}건 임베딩")
    logger.info("=" * 60)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="e약은요 데이터 적재")
    parser.add_argument("--max-pages", type=int, default=None, help="최대 페이지 수 (테스트용)")
    parser.add_argument("--resume", action="store_true", help="중단 후 재개")
    parser.add_argument("--batch-size", type=int, default=100, help="페이지당 건수")
    args = parser.parse_args()

    asyncio.run(main(max_pages=args.max_pages, resume=args.resume, batch_size=args.batch_size))
