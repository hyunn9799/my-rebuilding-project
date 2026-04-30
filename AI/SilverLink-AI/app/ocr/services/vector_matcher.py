"""Vector-based medication-name matcher backed by ChromaDB."""

import time

from loguru import logger

from app.chatbot.services.embedding_service import EmbeddingService
from app.ocr.model.drug_model import MatchCandidate, MatchResult
from app.ocr.repository.drug_repository import DrugRepository
from app.ocr.repository.drug_vector_repository import DrugVectorRepository


class VectorMatcher:
    """Match normalized medication names using vector similarity."""

    def __init__(
        self,
        drug_repository: DrugRepository,
        vector_repository: DrugVectorRepository,
        embedding_service: EmbeddingService,
    ):
        self.drug_repo = drug_repository
        self.vector_repo = vector_repository
        self.embedding_service = embedding_service

    def match(self, normalized_name: str, top_k: int = 5) -> MatchResult:
        if not normalized_name:
            return MatchResult()

        started = time.perf_counter()
        try:
            query_embedding = self.embedding_service.create_embedding(normalized_name)
            results = self.vector_repo.search_similar(query_embedding, top_k=top_k)

            if not results:
                elapsed_ms = (time.perf_counter() - started) * 1000
                logger.info(
                    "VectorDB match returned no results: query='{}', top_k={}, elapsed_ms={:.1f}",
                    normalized_name,
                    top_k,
                    elapsed_ms,
                )
                return MatchResult()

            candidates = []
            for item_seq, distance, metadata in results:
                similarity = max(0.0, min(1.0, 1.0 - distance))

                drug_results = self.drug_repo.exact_match(metadata.get("item_name", ""))
                if drug_results:
                    drug_info = drug_results[0][0]
                else:
                    from app.ocr.model.drug_model import DrugInfo

                    drug_info = DrugInfo(
                        item_seq=item_seq,
                        item_name=metadata.get("item_name", item_seq),
                        entp_name=metadata.get("entp_name"),
                    )

                candidates.append(
                    MatchCandidate(
                        drug_info=drug_info,
                        score=round(similarity, 3),
                        method="vector",
                        evidence={
                            "source": "vector_db",
                            "distance": round(distance, 4),
                            "name_match": round(similarity, 3),
                        },
                    )
                )

            candidates.sort(key=lambda c: c.score, reverse=True)
            best_score = candidates[0].score if candidates else 0.0
            elapsed_ms = (time.perf_counter() - started) * 1000
            logger.info(
                "VectorDB match completed: query='{}', candidates={}, best_score={}, elapsed_ms={:.1f}",
                normalized_name,
                len(candidates),
                best_score,
                elapsed_ms,
            )

            return MatchResult(
                candidates=candidates,
                best_score=best_score,
                method="vector",
            )

        except Exception as exc:
            elapsed_ms = (time.perf_counter() - started) * 1000
            logger.error("VectorMatcher failed: error={}, elapsed_ms={:.1f}", exc, elapsed_ms)
            return MatchResult()
