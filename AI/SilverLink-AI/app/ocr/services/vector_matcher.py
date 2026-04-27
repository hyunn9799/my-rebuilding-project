"""
벡터 기반 약품명 매칭 서비스 (ChromaDB)
MySQL 1차 매칭에서 score < threshold인 경우 2차로 호출
"""
from typing import List
from loguru import logger

from app.core.config import configs
from app.ocr.repository.drug_repository import DrugRepository
from app.ocr.repository.drug_vector_repository import DrugVectorRepository
from app.ocr.model.drug_model import MatchCandidate, MatchResult
from app.chatbot.services.embedding_service import EmbeddingService


class VectorMatcher:
    """ChromaDB 임베딩 유사도 기반 약품 매칭"""

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
        """
        임베딩 유사도로 약품 매칭

        Args:
            normalized_name: 정규화된 약품명
            top_k: 반환할 후보 수

        Returns:
            MatchResult
        """
        if not normalized_name:
            return MatchResult()

        try:
            # 1. 입력 텍스트 임베딩 생성
            query_embedding = self.embedding_service.create_embedding(normalized_name)

            # 2. ChromaDB 유사도 검색
            results = self.vector_repo.search_similar(query_embedding, top_k=top_k)

            if not results:
                logger.info(f"VectorDB 매칭 결과 없음: '{normalized_name}'")
                return MatchResult()

            # 3. item_seq로 MySQL에서 상세 정보 조회
            candidates = []
            for item_seq, distance, metadata in results:
                # 코사인 거리 → 유사도 점수 (0~1, 1이 가장 유사)
                similarity = 1.0 - distance
                similarity = max(0.0, min(1.0, similarity))

                # MySQL에서 상세 정보 가져오기
                drug_results = self.drug_repo.exact_match(metadata.get("item_name", ""))
                if not drug_results:
                    # item_seq로 직접 조회 시도
                    from app.ocr.model.drug_model import DrugInfo
                    drug_info = DrugInfo(
                        item_seq=item_seq,
                        item_name=metadata.get("item_name", item_seq),
                        entp_name=metadata.get("entp_name"),
                    )
                else:
                    drug_info = drug_results[0][0]

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

            logger.info(
                f"VectorDB 매칭 완료: '{normalized_name}' → "
                f"{len(candidates)}건, best_score={best_score}"
            )

            return MatchResult(
                candidates=candidates,
                best_score=best_score,
                method="vector",
            )

        except Exception as e:
            logger.error(f"VectorMatcher 실패: {e}")
            return MatchResult()
