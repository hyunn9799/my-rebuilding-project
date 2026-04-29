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
from app.integration.drug_api.drug_api_client import DrugApiClient, get_field
from app.ocr.repository.drug_repository import DrugRepository
from app.ocr.repository.drug_vector_repository import DrugVectorRepository
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

def build_drug_embedding_text(drug: DrugInfo) -> str:
    """약품 임베딩 텍스트 생성 (효능 환각 방지)"""
    parts = [
        f"약품명: {drug.item_name}",
        f"주성분: {drug.item_ingr_name or '정보없음'}",
        f"제조사: {drug.entp_name or '정보없음'}",
        f"분류: {drug.spclty_pblc or '정보없음'}",
        f"제형/성상: {drug.chart or '정보없음'}",
        f"저장방법: {drug.storage_method or '정보없음'}",
    ]
    if drug.efcy_qesitm:
        parts.append(f"효능효과: {drug.efcy_qesitm}")
    else:
        parts.append("효능효과: 공식 효능 정보가 제공되지 않았습니다. (주성분으로 유추 금지)")
        
    return " | ".join(parts)

def parse_api_item(item: dict) -> DrugInfo:
    """허가정보 API 응답 아이템 → DrugInfo 변환 (get_field 헬퍼 사용)"""
    item_name = get_field(item, "ITEM_NAME", "item_name", default="")
    cancel_date = get_field(item, "CANCEL_DATE", "cancel_date")

    # 정책 P4: CANCEL_DATE 있으면 is_active=0
    is_active = 0 if cancel_date else 1

    item_seq = get_field(item, "ITEM_SEQ", "item_seq", default="")
    ingr_cnt = get_field(item, "ITEM_INGR_CNT", "item_ingr_cnt")

    return DrugInfo(
        item_seq=str(item_seq),
        item_name=item_name,
        item_name_normalized=normalize_drug_name(item_name),
        item_eng_name=get_field(item, "ITEM_ENG_NAME", "item_eng_name"),
        entp_name=get_field(item, "ENTP_NAME", "entp_name"),
        entp_eng_name=get_field(item, "ENTP_ENG_NAME", "entp_eng_name"),
        item_ingr_name=get_field(item, "ITEM_INGR_NAME", "MAIN_ITEM_INGR", "item_ingr_name"),
        item_ingr_cnt=int(ingr_cnt) if ingr_cnt else None,
        spclty_pblc=get_field(item, "SPCLTY_PBLC", "spclty_pblc"),
        prduct_type=get_field(item, "PRDUCT_TYPE", "prduct_type"),
        item_permit_date=get_field(item, "ITEM_PERMIT_DATE", "item_permit_date"),
        permit_date=get_field(item, "PERMIT_DATE", "permit_date"),
        permit_no=get_field(item, "PERMIT_NO", "permit_no"),
        cancel_date=cancel_date,
        cancel_name=get_field(item, "CANCEL_NAME", "cancel_name"),
        edi_code=get_field(item, "EDI_CODE", "edi_code"),
        permit_kind_code=get_field(item, "PERMIT_KIND_CODE", "permit_kind_code"),
        etc_otc_code=get_field(item, "ETC_OTC_CODE", "etc_otc_code"),
        chart=get_field(item, "CHART", "chart"),
        material_name=get_field(item, "MATERIAL_NAME", "material_name"),
        storage_method=get_field(item, "STORAGE_METHOD", "storage_method"),
        valid_term=get_field(item, "VALID_TERM", "valid_term"),
        pack_unit=get_field(item, "PACK_UNIT", "pack_unit"),
        raw_json=json.dumps(item, ensure_ascii=False) if isinstance(item, dict) else None,
        item_image=get_field(item, "BIG_PRDT_IMG_URL", "big_prdt_img_url"),
        is_active=is_active,
        # 분리 예정인 공공데이터 (향후 다른 API로 채움)
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


async def main(
    max_pages: int = None, 
    resume: bool = False, 
    batch_size: int = 100,
    chromadb_only: bool = False,
    active_only: bool = True
):
    """메인 적재 로직"""
    logger.info("=" * 60)
    logger.info("의약품 허가정보 데이터 적재 시작")
    logger.info("=" * 60)

    drug_repo = DrugRepository()
    
    if chromadb_only:
        from app.core.container import container
        import openai
        
        logger.info(f"--- ChromaDB 전용 적재 모드 (active_only={active_only}) ---")
        vector_repo = DrugVectorRepository()
        
        medications = drug_repo.fetch_active_medications_for_embedding() if active_only else drug_repo.fetch_all_medications_for_index()
        logger.info(f"대상 약품: {len(medications)}건")
        
        if not medications:
            logger.info("적재할 데이터가 없습니다.")
            return

        embedder = openai.AsyncOpenAI(api_key=os.getenv("OPENAI_API_KEY"))
        import math
        
        chunk_size = 500
        total_chunks = math.ceil(len(medications) / chunk_size)
        total_inserted = 0

        for i in range(total_chunks):
            chunk = medications[i * chunk_size:(i + 1) * chunk_size]
            texts = [build_drug_embedding_text(d) for d in chunk]
            item_seqs = [d.item_seq for d in chunk]
            metadatas = [{
                "item_name": d.item_name,
                "entp_name": d.entp_name or "",
                "ingr_name": d.item_ingr_name or "",
                "spclty_pblc": d.spclty_pblc or "",
            } for d in chunk]

            logger.info(f"임베딩 생성 중... ({i+1}/{total_chunks}) - {len(chunk)}건")
            try:
                response = await embedder.embeddings.create(
                    input=texts,
                    model=os.getenv("EMBEDDING_MODEL", "text-embedding-3-small")
                )
                embeddings = [e.embedding for e in response.data]
                
                inserted = vector_repo.bulk_upsert(
                    item_seqs=item_seqs,
                    embeddings=embeddings,
                    metadatas=metadatas,
                    batch_size=500
                )
                total_inserted += inserted
            except Exception as e:
                logger.error(f"임베딩 청크 {i+1} 처리 실패: {e}")
                
        logger.info(f"ChromaDB 적재 완료: 총 {total_inserted}/{len(medications)}건")
        return

    # 일반 API 추출 모드
    api_client = DrugApiClient()

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
    parser.add_argument("--chromadb-only", action="store_true", help="MySQL에서 데이터를 읽어 ChromaDB에 임베딩 적재만 수행")
    parser.add_argument("--all", action="store_true", help="ChromaDB 적재 시 취소된 약품을 포함해 전체를 적재합니다 (기본값: 활성 약품만)")
    args = parser.parse_args()

    # argparse flag logic override defaults
    active_only = not args.all

    asyncio.run(main(
        max_pages=args.max_pages, 
        resume=args.resume, 
        batch_size=args.batch_size,
        chromadb_only=args.chromadb_only,
        active_only=active_only
    ))
