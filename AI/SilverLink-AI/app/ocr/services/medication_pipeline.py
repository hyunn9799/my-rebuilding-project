"""
OCR 약 식별 파이프라인 오케스트레이터
7단계: OCR → 정규화 → MySQL 1차 → VectorDB 2차 → 규칙 검증 → Pseudo-Confidence → LLM 설명
"""
import time
from typing import Any, List, Optional, Tuple
from loguru import logger

from app.core.config import configs
from app.ocr.model.drug_model import (
    DecisionStatus,
    MatchCandidate,
    NormalizedDrug,
    OCRToken,
    PipelineResult,
    PipelineStage,
)
from app.ocr.services.llm_descriptor import LLMDescriptor
from app.ocr.services.mysql_matcher import MySQLMatcher
from app.ocr.services.pseudo_confidence_scorer import PseudoConfidenceScorer
from app.ocr.services.rule_validator import RuleValidator
from app.ocr.services.text_normalizer import TextNormalizer


class MedicationPipeline:
    """
    OCR 약 식별 파이프라인 오케스트레이터

    파이프라인 흐름:
    ① OCR 텍스트 수신
    ② 정규화 (약품명 후보 추출)
    ③ MySQL 다중 매칭 (exact→alias→error_alias→prefix→ngram→fuzzy)
    ④ VectorDB 2차 매칭 (③에서 score < threshold인 경우만)
    ⑤ 규칙 검증 (성분/함량/업체/제형) — evidence 태깅만
    ⑥ Pseudo-Confidence 점수 재계산
    ⑦ LLM 설명 생성
    """

    LOW_OCR_CONFIDENCE_THRESHOLD = 0.75
    AMBIGUOUS_TOP_GAP = 0.08

    def __init__(
        self,
        text_normalizer: TextNormalizer,
        mysql_matcher: MySQLMatcher,
        vector_matcher: Any,
        rule_validator: RuleValidator,
        llm_descriptor: LLMDescriptor,
        pseudo_confidence_scorer: Optional[PseudoConfidenceScorer] = None,
        match_threshold: Optional[float] = None,
    ):
        self.normalizer = text_normalizer
        self.mysql_matcher = mysql_matcher
        self.vector_matcher = vector_matcher
        self.rule_validator = rule_validator
        self.llm_descriptor = llm_descriptor
        self.scorer = pseudo_confidence_scorer or PseudoConfidenceScorer()
        self.threshold = match_threshold or configs.DRUG_MATCH_THRESHOLD

    async def process(
        self,
        ocr_text: str,
        elderly_user_id: Optional[int] = None,
        ocr_tokens: Optional[List[OCRToken]] = None,
    ) -> PipelineResult:
        """
        전체 파이프라인 실행

        Args:
            ocr_text: OCR 원본 텍스트
            elderly_user_id: 어르신 사용자 ID (로깅용)

        Returns:
            PipelineResult
        """
        pipeline_start = time.perf_counter()
        stages: List[PipelineStage] = []
        all_candidates: List[MatchCandidate] = []
        normalized_drugs: List[NormalizedDrug] = []
        has_alias_conflict = False

        logger.info(f"파이프라인 시작 — user_id: {elderly_user_id}, text_len: {len(ocr_text)}")

        try:
            # ──────────────────────────────────
            # Stage 2: 정규화
            # ──────────────────────────────────
            t0 = time.perf_counter()
            normalized_drugs = self.normalizer.normalize(ocr_text, ocr_tokens=ocr_tokens)
            stage_ms = (time.perf_counter() - t0) * 1000

            stages.append(PipelineStage(
                stage="normalize",
                duration_ms=round(stage_ms, 2),
                result_summary=f"{len(normalized_drugs)}개 약품명 후보 추출",
            ))

            if not normalized_drugs:
                logger.warning("정규화 결과 약품명 후보 없음")
                return PipelineResult(
                    success=False,
                    raw_ocr_text=ocr_text,
                    pipeline_stages=stages,
                    total_duration_ms=round((time.perf_counter() - pipeline_start) * 1000, 2),
                    warnings=["OCR 텍스트에서 약품명을 추출할 수 없습니다."],
                    decision_status="NOT_FOUND",
                    decision_reasons=["정규화 단계에서 약품명 후보가 추출되지 않았습니다."],
                )

            # ──────────────────────────────────
            # Stage 3: MySQL 다중 매칭 (1차)
            # ──────────────────────────────────
            t0 = time.perf_counter()
            mysql_results = {}
            for nd in normalized_drugs:
                result = self.mysql_matcher.match(nd.name)
                mysql_results[nd.name] = result
                all_candidates.extend(result.candidates)
                if result.alias_conflict:
                    has_alias_conflict = True
            stage_ms = (time.perf_counter() - t0) * 1000

            best_mysql_score = max(
                (r.best_score for r in mysql_results.values()), default=0.0
            )
            best_mysql_method = "none"
            for r in mysql_results.values():
                if r.best_score == best_mysql_score and r.method != "none":
                    best_mysql_method = r.method
                    break

            stages.append(PipelineStage(
                stage="mysql_match",
                duration_ms=round(stage_ms, 2),
                result_summary=(
                    f"best_score={best_mysql_score:.3f}, method={best_mysql_method}, "
                    f"candidates={len(all_candidates)}, alias_conflict={has_alias_conflict}"
                ),
            ))

            # ──────────────────────────────────
            # Stage 4: VectorDB 2차 (score < threshold인 경우만)
            # ──────────────────────────────────
            if best_mysql_score < self.threshold:
                t0 = time.perf_counter()
                for nd in normalized_drugs:
                    mysql_r = mysql_results.get(nd.name)
                    if mysql_r and mysql_r.best_score < self.threshold:
                        vector_result = self.vector_matcher.match(nd.name)
                        all_candidates.extend(vector_result.candidates)
                stage_ms = (time.perf_counter() - t0) * 1000

                stages.append(PipelineStage(
                    stage="vector_match",
                    duration_ms=round(stage_ms, 2),
                    result_summary=f"VectorDB 2차 실행, total_candidates={len(all_candidates)}",
                ))
            else:
                stages.append(PipelineStage(
                    stage="vector_match",
                    duration_ms=0.0,
                    result_summary=f"SKIP (mysql_score={best_mysql_score:.3f} ≥ {self.threshold})",
                ))

            # ──────────────────────────────────
            # Stage 5: 규칙 검증 (evidence 태깅)
            # ──────────────────────────────────
            t0 = time.perf_counter()
            unique_candidates = self._deduplicate(all_candidates)[:10]
            validated_candidates = []
            for nd in normalized_drugs:
                validated = self.rule_validator.validate(
                    unique_candidates, ocr_text, nd
                )
                validated_candidates.extend(validated)

            validated_unique = self._deduplicate(validated_candidates)[:10]
            stage_ms = (time.perf_counter() - t0) * 1000

            stages.append(PipelineStage(
                stage="rule_validate",
                duration_ms=round(stage_ms, 2),
                result_summary=f"검증 완료, validated={len(validated_unique)}건",
            ))

            # ──────────────────────────────────
            # Stage 6: Pseudo-Confidence 점수 재계산
            # ──────────────────────────────────
            t0 = time.perf_counter()
            scored_candidates = []
            for nd in normalized_drugs:
                scored = self.scorer.score(validated_unique, nd)
                scored_candidates.extend(scored)

            final_candidates = self._deduplicate(scored_candidates)[:5]
            stage_ms = (time.perf_counter() - t0) * 1000

            stages.append(PipelineStage(
                stage="pseudo_confidence",
                duration_ms=round(stage_ms, 2),
                result_summary=f"점수 재계산 완료, final={len(final_candidates)}건",
            ))

            # ──────────────────────────────────
            # Stage 7: 최종 판정
            # ──────────────────────────────────
            t0 = time.perf_counter()
            decision_status, match_confidence, requires_confirmation, decision_reasons = (
                self._decide(final_candidates, normalized_drugs, has_alias_conflict)
            )
            stage_ms = (time.perf_counter() - t0) * 1000

            stages.append(PipelineStage(
                stage="decision",
                duration_ms=round(stage_ms, 2),
                result_summary=(
                    f"status={decision_status}, confidence={match_confidence:.3f}, "
                    f"requires_confirmation={requires_confirmation}"
                ),
            ))

            # ──────────────────────────────────
            # Stage 8: LLM 설명 생성
            # ──────────────────────────────────
            t0 = time.perf_counter()
            llm_candidates = final_candidates if decision_status == "MATCHED" else final_candidates[:3]
            llm_result = await self.llm_descriptor.generate_description(
                llm_candidates,
                ocr_text,
                decision_status=decision_status,
            )
            stage_ms = (time.perf_counter() - t0) * 1000

            stages.append(PipelineStage(
                stage="llm_describe",
                duration_ms=round(stage_ms, 2),
                result_summary=f"설명 생성 완료, {len(llm_result.get('medications', []))}개 약품",
            ))

            # ──────────────────────────────────
            # 최종 결과 조립
            # ──────────────────────────────────
            total_ms = (time.perf_counter() - pipeline_start) * 1000

            logger.info(
                f"파이프라인 완료 — {len(final_candidates)}개 약품 식별, "
                f"총 {total_ms:.0f}ms"
            )

            return PipelineResult(
                success=decision_status in ("MATCHED", "AMBIGUOUS", "NEED_USER_CONFIRMATION"),
                identified_drugs=final_candidates,
                raw_ocr_text=ocr_text,
                normalized_names=normalized_drugs,
                llm_description=llm_result.get("description", ""),
                warnings=llm_result.get("warnings", []),
                pipeline_stages=stages,
                total_duration_ms=round(total_ms, 2),
                decision_status=decision_status,
                match_confidence=match_confidence,
                requires_user_confirmation=requires_confirmation,
                decision_reasons=decision_reasons,
            )

        except Exception as e:
            total_ms = (time.perf_counter() - pipeline_start) * 1000
            logger.error(f"파이프라인 에러: {e}")

            return PipelineResult(
                success=False,
                raw_ocr_text=ocr_text,
                normalized_names=normalized_drugs,
                pipeline_stages=stages,
                total_duration_ms=round(total_ms, 2),
                error_message=str(e),
                warnings=["파이프라인 처리 중 오류가 발생했습니다."],
                decision_status="NOT_FOUND",
                decision_reasons=[str(e)],
            )

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

    def _decide(
        self,
        candidates: List[MatchCandidate],
        normalized_drugs: List[NormalizedDrug],
        alias_conflict: bool = False,
    ) -> Tuple[DecisionStatus, float, bool, List[str]]:
        """후보 점수와 규칙 검증 결과로 최종 상태를 판정."""
        if not candidates:
            return "NOT_FOUND", 0.0, False, ["유효한 약품 후보가 없습니다."]

        top = candidates[0]
        second = candidates[1] if len(candidates) > 1 else None
        reasons: List[str] = [f"최고 후보: {top.drug_info.item_name} ({top.score:.3f})"]

        # Alias 충돌
        if alias_conflict:
            conflict_names = [c.drug_info.item_name for c in candidates[:3]]
            reasons.append(
                f"alias 충돌 — 동일 별칭이 복수 약품에 매핑됩니다: {', '.join(conflict_names)}"
            )
            return "AMBIGUOUS", top.score, True, reasons

        low_ocr = [
            nd.ocr_confidence
            for nd in normalized_drugs
            if nd.ocr_confidence is not None and nd.ocr_confidence < self.LOW_OCR_CONFIDENCE_THRESHOLD
        ]
        if low_ocr:
            reasons.append(f"OCR 신뢰도 낮음: min={min(low_ocr):.3f}")
            return "LOW_CONFIDENCE", top.score, True, reasons

        if top.evidence.get("strength_match") is False:
            reasons.append("함량이 다른 후보일 가능성이 있습니다.")
            return "NEED_USER_CONFIRMATION", top.score, True, reasons

        if second and (top.score - second.score) < self.AMBIGUOUS_TOP_GAP:
            reasons.append(
                f"상위 후보 점수 차이가 작습니다: 2순위={second.drug_info.item_name} ({second.score:.3f})"
            )
            return "AMBIGUOUS", top.score, True, reasons

        if top.score < self.threshold:
            reasons.append(f"최고 점수가 threshold({self.threshold:.2f})보다 낮습니다.")
            return "LOW_CONFIDENCE", top.score, True, reasons

        reasons.append("점수와 규칙 검증 기준을 통과했습니다.")
        return "MATCHED", top.score, False, reasons
