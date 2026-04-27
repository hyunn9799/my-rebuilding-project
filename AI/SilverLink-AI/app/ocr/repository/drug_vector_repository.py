"""
약품 벡터 검색 레포지토리 (ChromaDB)
"""
import chromadb
from typing import List, Tuple, Optional
from loguru import logger

from app.core.config import configs
from app.ocr.model.drug_model import DrugInfo


class DrugVectorRepository:
    """ChromaDB 기반 약품명 임베딩 벡터 검색"""

    def __init__(self, persist_directory: Optional[str] = None, collection_name: Optional[str] = None):
        self.persist_directory = persist_directory or configs.CHROMA_PERSIST_DIRECTORY
        self.collection_name = collection_name or configs.DRUG_COLLECTION_NAME

        self._client = chromadb.PersistentClient(path=self.persist_directory)
        self._collection = self._client.get_or_create_collection(
            name=self.collection_name,
            metadata={"hnsw:space": "cosine"},
        )
        logger.info(
            f"ChromaDB 초기화: collection='{self.collection_name}', "
            f"count={self._collection.count()}, path='{self.persist_directory}'"
        )

    def search_similar(
        self,
        query_embedding: List[float],
        top_k: int = 5,
    ) -> List[Tuple[str, float, dict]]:
        """
        코사인 유사도로 유사 약품 검색

        Returns:
            [(item_seq, distance, metadata), ...]
            distance는 코사인 거리 (0에 가까울수록 유사)
        """
        if self._collection.count() == 0:
            logger.warning("ChromaDB 컬렉션이 비어있습니다. 데이터 적재가 필요합니다.")
            return []

        try:
            results = self._collection.query(
                query_embeddings=[query_embedding],
                n_results=min(top_k, self._collection.count()),
                include=["metadatas", "distances"],
            )

            items = []
            if results and results["ids"] and results["ids"][0]:
                for i, item_seq in enumerate(results["ids"][0]):
                    distance = results["distances"][0][i] if results["distances"] else 1.0
                    metadata = results["metadatas"][0][i] if results["metadatas"] else {}
                    items.append((item_seq, distance, metadata))

            return items
        except Exception as e:
            logger.error(f"ChromaDB 검색 실패: {e}")
            return []

    def upsert_embedding(
        self,
        item_seq: str,
        embedding: List[float],
        metadata: Optional[dict] = None,
    ) -> bool:
        """단건 임베딩 upsert"""
        try:
            self._collection.upsert(
                ids=[item_seq],
                embeddings=[embedding],
                metadatas=[metadata or {}],
            )
            return True
        except Exception as e:
            logger.error(f"ChromaDB upsert 실패 (item_seq={item_seq}): {e}")
            return False

    def bulk_upsert(
        self,
        item_seqs: List[str],
        embeddings: List[List[float]],
        metadatas: Optional[List[dict]] = None,
        batch_size: int = 500,
    ) -> int:
        """대량 임베딩 upsert (배치 처리)"""
        if not metadatas:
            metadatas = [{}] * len(item_seqs)

        # ── 중복 ID 제거 (마지막 항목 유지) ──
        original_count = len(item_seqs)
        seen: dict[str, int] = {}
        for idx, seq in enumerate(item_seqs):
            seen[seq] = idx  # 같은 ID면 뒤쪽 인덱스로 덮어씀

        unique_indices = sorted(seen.values())
        item_seqs = [item_seqs[i] for i in unique_indices]
        embeddings = [embeddings[i] for i in unique_indices]
        metadatas = [metadatas[i] for i in unique_indices]

        if len(item_seqs) < original_count:
            logger.warning(f"ChromaDB 중복 ID 제거: {original_count}건 → {len(item_seqs)}건")

        total = len(item_seqs)
        count = 0

        for i in range(0, total, batch_size):
            batch_ids = item_seqs[i:i + batch_size]
            batch_embeddings = embeddings[i:i + batch_size]
            batch_metadatas = metadatas[i:i + batch_size]

            try:
                self._collection.upsert(
                    ids=batch_ids,
                    embeddings=batch_embeddings,
                    metadatas=batch_metadatas,
                )
                count += len(batch_ids)
                logger.info(f"ChromaDB bulk_upsert: {count}/{total}")
            except Exception as e:
                logger.error(f"ChromaDB batch upsert 실패 (offset={i}): {e}")

        return count

    def get_count(self) -> int:
        """컬렉션 내 문서 수"""
        return self._collection.count()
