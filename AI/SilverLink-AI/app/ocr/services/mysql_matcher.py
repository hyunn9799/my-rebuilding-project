"""
MySQL 기반 약품명 다중 매칭 서비스
LocalDrugIndex 우선, 실패 시 MySQL fallback 실행
"""
from typing import List, Optional, Set
from loguru import logger
from rapidfuzz import fuzz

from app.core.config import configs
from app.ocr.repository.drug_repository import DrugRepository
from app.ocr.model.drug_model import DrugInfo, MatchCandidate, MatchResult
from app.ocr.services.drug_dictionary_index import DrugDictionaryIndex


class MySQLMatcher:
    """MySQL 기반 순차 매칭"""

    def __init__(
        self,
        drug_repository: DrugRepository,
        threshold: Optional[float] = None,
        dictionary_index: Optional[DrugDictionaryIndex] = None,
    ):
        self.repo = drug_repository
        self.threshold = threshold or configs.DRUG_MATCH_THRESHOLD
        self.dictionary_index = dictionary_index

    def match(self, normalized_name: str) -> MatchResult:
        """
        순차 매칭 수행. threshold 이상이면 즉시 반환.

        Args:
            normalized_name: 정규화된 약품명

        Returns:
            MatchResult (candidates, best_score, method, alias_conflict)
        """
        if not normalized_name or len(normalized_name) < 2:
            return MatchResult()

        local_result = self._try_local_index(normalized_name)
        if local_result is not None:
            if local_result.candidates and local_result.best_score >= self.threshold:
                logger.info(
                    "Local dictionary match succeeded: '{}' -> method={}, score={}",
                    normalized_name,
                    local_result.method,
                    local_result.best_score,
                )
                return local_result

            if local_result.candidates:
                fallback_result = self._match_mysql_fallback(normalized_name, include_fuzzy=False)
                merged = self._deduplicate([*local_result.candidates, *fallback_result.candidates])
                best = self._get_best(merged)
                return MatchResult(
                    candidates=merged,
                    best_score=best.score if best else 0.0,
                    method=best.method if best else fallback_result.method,
                    alias_conflict=local_result.alias_conflict or fallback_result.alias_conflict,
                )

            return self._match_mysql_fallback(normalized_name, include_fuzzy=False)

        return self._match_mysql_fallback(normalized_name, include_fuzzy=True)

    def _try_local_index(self, normalized_name: str) -> Optional[MatchResult]:
        if not self.dictionary_index:
            return None
        try:
            result = self.dictionary_index.match(normalized_name, self.threshold)
        except Exception as exc:
            logger.warning("Local dictionary lookup failed; using MySQL fallback: {}", exc)
            return None
        if result.method == "local_unavailable":
            return None
        return result

    def _match_mysql_fallback(self, normalized_name: str, include_fuzzy: bool = True) -> MatchResult:
        # Stage 1: exact match
        candidates = self._try_exact(normalized_name)
        if candidates and candidates[0].score >= self.threshold:
            logger.info(f"MySQL exact 매칭 성공: '{normalized_name}' → score={candidates[0].score}")
            return MatchResult(
                candidates=candidates,
                best_score=candidates[0].score,
                method="exact",
            )

        # Stage 2: alias match
        alias_candidates = self._try_alias(normalized_name)
        candidates.extend(alias_candidates)
        alias_conflict = self._has_alias_conflict(alias_candidates)
        best = self._get_best(candidates)
        if best and best.score >= self.threshold:
            logger.info(
                f"MySQL alias 매칭 성공: '{normalized_name}' → "
                f"score={best.score}, conflict={alias_conflict}"
            )
            return MatchResult(
                candidates=self._deduplicate(candidates),
                best_score=best.score,
                method="alias",
                alias_conflict=alias_conflict,
            )

        # Stage 3: OCR error alias match
        error_alias_candidates = self._try_error_alias(normalized_name)
        candidates.extend(error_alias_candidates)
        error_alias_conflict = self._has_alias_conflict(error_alias_candidates)
        alias_conflict = alias_conflict or error_alias_conflict
        best = self._get_best(candidates)
        if best and best.score >= self.threshold:
            logger.info(
                f"MySQL error_alias 매칭 성공: '{normalized_name}' → "
                f"score={best.score}, conflict={alias_conflict}"
            )
            return MatchResult(
                candidates=self._deduplicate(candidates),
                best_score=best.score,
                method="error_alias",
                alias_conflict=alias_conflict,
            )

        # Stage 4: prefix match
        prefix_candidates = self._try_prefix(normalized_name)
        candidates.extend(prefix_candidates)
        best = self._get_best(candidates)
        if best and best.score >= self.threshold:
            logger.info(f"MySQL prefix 매칭 성공: '{normalized_name}' → score={best.score}")
            return MatchResult(
                candidates=self._deduplicate(candidates),
                best_score=best.score,
                method="prefix",
                alias_conflict=alias_conflict,
            )

        # Stage 5: ngram match
        ngram_candidates = self._try_ngram(normalized_name)
        candidates.extend(ngram_candidates)
        best = self._get_best(candidates)
        if best and best.score >= self.threshold:
            logger.info(f"MySQL ngram 매칭 성공: '{normalized_name}' → score={best.score}")
            return MatchResult(
                candidates=self._deduplicate(candidates),
                best_score=best.score,
                method="ngram",
                alias_conflict=alias_conflict,
            )

        # Stage 6: fuzzy match (Levenshtein)
        fuzzy_candidates = self._try_fuzzy(normalized_name) if include_fuzzy else []
        candidates.extend(fuzzy_candidates)
        best = self._get_best(candidates)

        final_method = "fuzzy" if fuzzy_candidates else "none"
        if best:
            logger.info(
                f"MySQL 매칭 완료: '{normalized_name}' → "
                f"best_score={best.score}, method={final_method}"
            )

        return MatchResult(
            candidates=self._deduplicate(candidates),
            best_score=best.score if best else 0.0,
            method=final_method,
            alias_conflict=alias_conflict,
        )

    # ------------------------------------------------------------------
    # Alias conflict detection
    # ------------------------------------------------------------------

    @staticmethod
    def _has_alias_conflict(candidates: List[MatchCandidate]) -> bool:
        """alias/error_alias 후보가 2개 이상 서로 다른 drug(item_seq)에 매핑되면 충돌."""
        if len(candidates) < 2:
            return False
        unique_seqs: Set[str] = {c.drug_info.item_seq for c in candidates}
        return len(unique_seqs) >= 2

    # ------------------------------------------------------------------
    # Stage helpers
    # ------------------------------------------------------------------

    def _try_exact(self, name: str) -> List[MatchCandidate]:
        """exact match"""
        results = self.repo.exact_match(name)
        return [
            MatchCandidate(
                drug_info=drug,
                score=score,
                method="exact",
                evidence={"name_match": score, "source": "mysql_fallback", "match_method": "mysql_exact"},
            )
            for drug, score in results
        ]

    def _try_alias(self, name: str) -> List[MatchCandidate]:
        """alias table exact match"""
        results = self.repo.alias_match(name)
        return [
            MatchCandidate(
                drug_info=drug,
                score=score,
                method="alias",
                evidence={
                    "name_match": score,
                    "source": "mysql_fallback",
                    "match_method": "mysql_alias",
                    **evidence,
                },
            )
            for drug, score, evidence in results
        ]

    def _try_error_alias(self, name: str) -> List[MatchCandidate]:
        """OCR error alias table exact match"""
        results = self.repo.error_alias_match(name)
        return [
            MatchCandidate(
                drug_info=drug,
                score=score,
                method="error_alias",
                evidence={
                    "name_match": score,
                    "source": "mysql_fallback",
                    "match_method": "mysql_error_alias",
                    **evidence,
                },
            )
            for drug, score, evidence in results
        ]

    def _try_prefix(self, name: str) -> List[MatchCandidate]:
        """prefix match"""
        results = self.repo.prefix_match(name)
        return [
            MatchCandidate(
                drug_info=drug,
                score=score,
                method="prefix",
                evidence={"name_match": score, "source": "mysql_fallback", "match_method": "mysql_prefix"},
            )
            for drug, score in results
        ]

    def _try_ngram(self, name: str) -> List[MatchCandidate]:
        """ngram fulltext match"""
        results = self.repo.ngram_match(name)
        return [
            MatchCandidate(
                drug_info=drug,
                score=score,
                method="ngram",
                evidence={"name_match": score, "source": "mysql_fallback", "match_method": "mysql_ngram"},
            )
            for drug, score in results
        ]

    def _try_fuzzy(self, name: str) -> List[MatchCandidate]:
        """fuzzy match using rapidfuzz Levenshtein"""
        all_drugs = self.repo.fetch_all_for_fuzzy()
        if not all_drugs:
            return []

        candidates = []
        for drug in all_drugs:
            target = drug.item_name_normalized or drug.item_name
            # rapidfuzz ratio (0~100) → 0.0~1.0
            ratio = fuzz.ratio(name, target) / 100.0
            # partial_ratio도 고려 (부분 문자열 매칭)
            partial = fuzz.partial_ratio(name, target) / 100.0
            # 가중 평균
            score = (ratio * 0.6 + partial * 0.4)

            if score >= 0.5:  # 최소 임계값
                candidates.append(
                    MatchCandidate(
                        drug_info=drug,
                        score=round(score, 3),
                        method="fuzzy",
                        evidence={
                            "name_match": round(score, 3),
                            "ratio": round(ratio, 3),
                            "partial_ratio": round(partial, 3),
                            "source": "mysql_fallback",
                            "match_method": "mysql_fuzzy",
                        },
                    )
                )

        # 상위 5개만
        candidates.sort(key=lambda c: c.score, reverse=True)
        return candidates[:5]

    def _get_best(self, candidates: List[MatchCandidate]) -> Optional[MatchCandidate]:
        """최고 점수 후보 반환"""
        if not candidates:
            return None
        return max(candidates, key=lambda c: c.score)

    def _deduplicate(self, candidates: List[MatchCandidate]) -> List[MatchCandidate]:
        """item_seq 기준 중복 제거 (최고 점수 유지)"""
        best_by_seq = {}
        for c in candidates:
            seq = c.drug_info.item_seq
            if seq not in best_by_seq or c.score > best_by_seq[seq].score:
                best_by_seq[seq] = c
        result = list(best_by_seq.values())
        result.sort(key=lambda c: c.score, reverse=True)
        return result
