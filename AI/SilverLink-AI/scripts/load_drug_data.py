"""
의약품 허가정보 API 데이터 → MySQL 적재 스크립트
(DrugPrdtPrmsnInfoService07 / getDrugPrdtPrmsnInq07)

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
from app.ocr.model.drug_model import DrugInfo

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
    """허가정보 API 응답 아이템 → DrugInfo 변환 (UPPER_CASE 필드)"""
    item_name = item.get("ITEM_NAME", "")
    cancel_date = item.get("CANCEL_DATE")

    # 정책 P4: CANCEL_DATE 있으면 is_active=0
    is_active = 0 if cancel_date else 1

    return DrugInfo(
        item_seq=str(item.get("ITEM_SEQ", "")),
        item_name=item_name,
        item_name_normalized=normalize_drug_name(item_name),
        item_eng_name=item.get("ITEM_ENG_NAME"),
        entp_name=item.get("ENTP_NAME"),
        entp_eng_name=item.get("ENTP_ENG_NAME"),
        item_ingr_name=item.get("ITEM_INGR_NAME"),
        item_ingr_cnt=int(item["ITEM_INGR_CNT"]) if item.get("ITEM_INGR_CNT") else None,
        spclty_pblc=item.get("SPCLTY_PBLC"),
        prduct_type=item.get("PRDUCT_TYPE"),
        item_permit_date=item.get("ITEM_PERMIT_DATE"),
        cancel_date=cancel_date,
        cancel_name=item.get("CANCEL_NAME"),
        edi_code=item.get("EDI_CODE"),
        permit_kind_code=item.get("PERMIT_KIND_CODE"),
        item_image=item.get("BIG_PRDT_IMG_URL"),
        is_active=is_active,
        # 새 API에 없는 필드 → None
        efcy_qesitm=None,
        use_method_qesitm=None,
        atpn_qesitm=None,
        intrc_qesitm=None,
        se_qesitm=None,
        deposit_method_qesitm=None,
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
    logger.info("의약품 허가정보 데이터 적재 시작")
    logger.info("=" * 60)

    # 초기화
    api_client = DrugApiClient()
    drug_repo = DrugRepository()

    # 테이블 생성
    drug_repo.create_table_if_not_exists()

    # 진행 상태 로드
    progress = load_progress() if resume else {"last_page": 0, "total_loaded": 0}
    start_page = progress["last_page"] + 1
    total_loaded = progress["total_loaded"]
    active_count = 0
    canceled_count = 0

    logger.info(f"시작 페이지: {start_page}, 기존 적재: {total_loaded}건")

    page_no = start_page
    consecutive_empty = 0

    while True:
        logger.info(f"--- 페이지 {page_no} 수집 중 ---")

        # API 호출 (item_name 없이 → 전체 조회)
        data = await api_client.search_by_name(
            item_name="",
            page_no=page_no,
            num_of_rows=batch_size,
        )

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

        # 활성/취소 카운트
        page_active = sum(1 for d in drugs if d.is_active == 1)
        page_canceled = sum(1 for d in drugs if d.is_active == 0)
        active_count += page_active
        canceled_count += page_canceled

        # MySQL 적재 (ChromaDB는 정책 P3에 따라 이번에 수행하지 않음)
        mysql_count = drug_repo.bulk_upsert(drugs)
        logger.info(
            f"MySQL 적재: {mysql_count}/{len(drugs)}건 "
            f"(활성: {page_active}, 취소: {page_canceled})"
        )

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
    logger.info(f"  활성(is_active=1): {active_count}건")
    logger.info(f"  취소(is_active=0): {canceled_count}건")
    logger.info(f"MySQL: medications_master 테이블")
    logger.info("ChromaDB: 이번 적재에서는 스킵 (정책 P3)")
    logger.info("=" * 60)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="의약품 허가정보 데이터 적재")
    parser.add_argument("--max-pages", type=int, default=None, help="최대 페이지 수 (테스트용)")
    parser.add_argument("--resume", action="store_true", help="중단 후 재개")
    parser.add_argument("--batch-size", type=int, default=100, help="페이지당 건수")
    args = parser.parse_args()

    asyncio.run(main(max_pages=args.max_pages, resume=args.resume, batch_size=args.batch_size))
