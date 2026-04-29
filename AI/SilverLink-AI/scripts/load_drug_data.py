"""
DrugPrdtPrmsnInfoService07 data loader.

Usage:
    python -m scripts.load_drug_data
    python -m scripts.load_drug_data --max-pages 5
    python -m scripts.load_drug_data --resume
    python -m scripts.load_drug_data --chromadb-only --reset-chromadb
"""
import argparse
import asyncio
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from dotenv import load_dotenv

load_dotenv()

from loguru import logger

from app.chatbot.services.embedding_service import EmbeddingService
from app.integration.drug_api.drug_api_client import DrugApiClient, get_field
from app.ocr.model.drug_model import DrugInfo
from app.ocr.repository.drug_repository import DrugRepository
from app.ocr.repository.drug_vector_repository import DrugVectorRepository


PROGRESS_FILE = "scripts/.load_progress.json"


def normalize_drug_name(name: str) -> str:
    """Normalize a drug name for lookup indexes."""
    name = re.sub(r"\([^)]*\)", "", name)
    name = re.sub(r"[^\w가-힣a-zA-Z0-9\s]", "", name)
    name = re.sub(r"\s+", " ", name).strip()
    return name


def build_vector_text(drug: DrugInfo) -> str:
    """Build deterministic text for drug embedding without inventing efficacy."""
    parts = [
        f"drug_name: {drug.item_name}",
        f"normalized_name: {drug.item_name_normalized or ''}",
        f"english_name: {drug.item_eng_name or ''}",
        f"ingredient: {drug.item_ingr_name or ''}",
        f"manufacturer: {drug.entp_name or ''}",
        f"classification: {drug.spclty_pblc or ''}",
        f"product_type: {drug.prduct_type or ''}",
        f"appearance: {drug.chart or ''}",
        f"storage: {drug.storage_method or ''}",
    ]
    if drug.efcy_qesitm:
        parts.append(f"efficacy: {drug.efcy_qesitm}")
    else:
        parts.append("efficacy: official efficacy information is not available")
    return " | ".join(part for part in parts if part and not part.endswith(": "))


def build_vector_metadata(drug: DrugInfo) -> dict:
    """Build ChromaDB metadata from a medication row."""
    return {
        "item_seq": drug.item_seq,
        "item_name": drug.item_name,
        "item_name_normalized": drug.item_name_normalized or "",
        "entp_name": drug.entp_name or "",
        "item_ingr_name": drug.item_ingr_name or "",
        "spclty_pblc": drug.spclty_pblc or "",
        "prduct_type": drug.prduct_type or "",
        "is_active": int(drug.is_active or 0),
    }


def fetch_vector_drugs(drug_repo: DrugRepository, active_only: bool = True) -> list[DrugInfo]:
    """Fetch drugs for ChromaDB loading."""
    if active_only:
        return drug_repo.fetch_all_medications_for_index()

    connection = None
    try:
        connection = drug_repo._get_connection()
        with connection.cursor() as cursor:
            cursor.execute("SELECT * FROM medications_master")
            rows = cursor.fetchall()
            return [drug_repo._row_to_drug_info(row) for row in rows]
    finally:
        if connection:
            connection.close()


def load_chromadb_from_mysql(
    limit: int | None = None,
    embedding_batch_size: int = 100,
    upsert_batch_size: int = 500,
    reset: bool = False,
    dry_run: bool = False,
    active_only: bool = True,
) -> int:
    """Load medication embeddings from MySQL into ChromaDB."""
    drug_repo = DrugRepository()
    drugs = fetch_vector_drugs(drug_repo, active_only=active_only)
    if limit is not None:
        drugs = drugs[:limit]

    logger.info("=" * 60)
    logger.info("ChromaDB drug embedding load from MySQL")
    logger.info("Target drugs: {} (active_only={})", len(drugs), active_only)
    logger.info("Dry run: {}", dry_run)
    logger.info("=" * 60)

    if dry_run:
        for drug in drugs[:5]:
            logger.info(
                "DRY-RUN item_seq={} text='{}'",
                drug.item_seq,
                build_vector_text(drug)[:180],
            )
        return len(drugs)

    vector_repo = DrugVectorRepository()
    if reset:
        vector_repo.reset_collection()

    embedding_service = EmbeddingService()
    total_upserted = 0

    for start in range(0, len(drugs), embedding_batch_size):
        batch = drugs[start:start + embedding_batch_size]
        texts = [build_vector_text(drug) for drug in batch]
        item_seqs = [drug.item_seq for drug in batch]
        metadatas = [build_vector_metadata(drug) for drug in batch]

        embeddings = embedding_service.create_embeddings(texts)
        total_upserted += vector_repo.bulk_upsert(
            item_seqs=item_seqs,
            embeddings=embeddings,
            metadatas=metadatas,
            batch_size=upsert_batch_size,
        )
        logger.info(
            "ChromaDB embedding progress: {}/{}",
            min(start + len(batch), len(drugs)),
            len(drugs),
        )

    logger.info(
        "ChromaDB load completed: upserted={}, collection_count={}",
        total_upserted,
        vector_repo.get_count(),
    )
    return total_upserted


def parse_api_item(item: dict) -> DrugInfo:
    """Convert a drug API item into DrugInfo."""
    item_name = get_field(item, "ITEM_NAME", "item_name", default="")
    cancel_date = get_field(item, "CANCEL_DATE", "cancel_date")
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
        is_active=0 if cancel_date else 1,
        efcy_qesitm=None,
        use_method_qesitm=None,
        atpn_qesitm=None,
        intrc_qesitm=None,
        se_qesitm=None,
        deposit_method_qesitm=None,
    )


def save_progress(page_no: int, total_loaded: int):
    os.makedirs(os.path.dirname(PROGRESS_FILE), exist_ok=True)
    with open(PROGRESS_FILE, "w", encoding="utf-8") as f:
        json.dump({"last_page": page_no, "total_loaded": total_loaded}, f)


def load_progress() -> dict:
    if os.path.exists(PROGRESS_FILE):
        with open(PROGRESS_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return {"last_page": 0, "total_loaded": 0}


async def main(max_pages: int = None, resume: bool = False, batch_size: int = 100):
    """Load public drug API data into MySQL."""
    logger.info("=" * 60)
    logger.info("Drug public API MySQL load started")
    logger.info("=" * 60)

    api_client = DrugApiClient()
    drug_repo = DrugRepository()
    drug_repo.create_table_if_not_exists()

    progress = load_progress() if resume else {"last_page": 0, "total_loaded": 0}
    start_page = progress["last_page"] + 1
    total_loaded = progress["total_loaded"]
    active_count = 0
    canceled_count = 0

    logger.info("Start page: {}, already loaded: {}", start_page, total_loaded)

    page_no = start_page
    consecutive_empty = 0

    while True:
        logger.info("--- Loading page {} ---", page_no)

        data = await api_client.search_by_name(
            item_name="",
            page_no=page_no,
            num_of_rows=batch_size,
        )
        items = api_client._extract_items(data)

        if not items:
            consecutive_empty += 1
            if consecutive_empty >= 3:
                logger.info("Three consecutive empty pages; stopping")
                break
            page_no += 1
            continue

        consecutive_empty = 0
        drugs = [parse_api_item(item) for item in items]

        page_active = sum(1 for drug in drugs if drug.is_active == 1)
        page_canceled = sum(1 for drug in drugs if drug.is_active == 0)
        active_count += page_active
        canceled_count += page_canceled

        mysql_count = drug_repo.bulk_upsert(drugs)
        logger.info(
            "MySQL upsert: {}/{} (active={}, canceled={})",
            mysql_count,
            len(drugs),
            page_active,
            page_canceled,
        )

        total_loaded += len(drugs)
        save_progress(page_no, total_loaded)
        logger.info("Total loaded: {} (page {})", total_loaded, page_no)

        if max_pages and page_no >= start_page + max_pages - 1:
            logger.info("Max pages reached: {}", max_pages)
            break

        total_count = api_client._extract_total_count(data)
        if total_count and total_loaded >= total_count:
            logger.info("All API rows loaded: {}", total_count)
            break

        page_no += 1

    logger.info("=" * 60)
    logger.info("Load completed: total={}", total_loaded)
    logger.info("  active: {}", active_count)
    logger.info("  canceled: {}", canceled_count)
    logger.info("MySQL: medications_master")
    logger.info("ChromaDB: skipped in normal API load")
    logger.info("=" * 60)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Drug public API data loader")
    parser.add_argument("--max-pages", type=int, default=None, help="Maximum pages for API load")
    parser.add_argument("--resume", action="store_true", help="Resume from saved API load progress")
    parser.add_argument("--batch-size", type=int, default=100, help="Rows per API page")
    parser.add_argument("--chromadb-only", action="store_true", help="Load MySQL drugs into ChromaDB only")
    parser.add_argument("--all", action="store_true", help="Include inactive/canceled drugs in ChromaDB load")
    parser.add_argument("--dry-run", action="store_true", help="Print ChromaDB targets without creating embeddings")
    parser.add_argument("--limit", type=int, default=None, help="Limit ChromaDB target rows for smoke tests")
    parser.add_argument("--embedding-batch-size", type=int, default=100, help="OpenAI embedding batch size")
    parser.add_argument("--upsert-batch-size", type=int, default=500, help="ChromaDB upsert batch size")
    parser.add_argument("--reset-chromadb", action="store_true", help="Drop and recreate the ChromaDB collection")
    args = parser.parse_args()

    if args.chromadb_only:
        load_chromadb_from_mysql(
            limit=args.limit,
            embedding_batch_size=args.embedding_batch_size,
            upsert_batch_size=args.upsert_batch_size,
            reset=args.reset_chromadb,
            dry_run=args.dry_run,
            active_only=not args.all,
        )
    else:
        asyncio.run(main(max_pages=args.max_pages, resume=args.resume, batch_size=args.batch_size))
