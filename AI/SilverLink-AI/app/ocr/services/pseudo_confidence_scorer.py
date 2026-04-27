"""
Entity-weighted pseudo-confidence scorer.

OCR confidence가 없거나 약한 상황에서도 검색 근거 기반으로
신뢰도를 산출한다. 용량/성분 충돌은 강한 패널티를 적용한다.

RuleValidator가 태깅한 evidence를 참조하여 최종 점수와
breakdown을 계산한다.
"""
from typing import List, Optional

from loguru import logger

from app.ocr.model.drug_model import MatchCandidate, NormalizedDrug, ScoreBreakdown


class PseudoConfidenceScorer:
    """Entity-weighted pseudo-confidence 점수 계산기."""

    # ── 가중치 ──────────────────────────────────
    W_NAME = 0.30
    W_STRENGTH = 0.25
    W_UNIT = 0.15
    W_FORM = 0.10
    W_MANUFACTURER = 0.10
    W_TOP_GAP = 0.10

    # ── 패널티 ──────────────────────────────────
    PENALTY_STRENGTH_MISMATCH = -0.40
    PENALTY_LOW_OCR_CONFIDENCE = -0.10

    # ── top gap 정규화 구간 ────────────────────
    TOP_GAP_MAX = 0.30  # 이 이상이면 gap 보너스 최대

    def score(
        self,
        candidates: List[MatchCandidate],
        normalized_drug: Optional[NormalizedDrug] = None,
    ) -> List[MatchCandidate]:
        """
        후보 리스트를 받아 pseudo-confidence 점수를 재계산한다.

        2-pass 방식:
        1) entity 점수만으로 기본 점수 계산 (gap 제외) → 정렬
        2) 정렬 후 top1-top2 gap 보너스를 top 후보에만 적용

        Args:
            candidates: RuleValidator가 evidence를 태깅한 후보 리스트
            normalized_drug: 정규화된 약품명 정보

        Returns:
            점수가 재계산된 후보 리스트 (내림차순 정렬)
        """
        if not candidates:
            return candidates

        # ── Pass 1: entity 점수 계산 (gap 보너스 없이) ──
        pass1: List[MatchCandidate] = []
        for candidate in candidates:
            evidence = dict(candidate.evidence)
            breakdown = self._calculate_breakdown(candidate, normalized_drug, top_gap=0.0)
            evidence["score_breakdown"] = breakdown.model_dump()

            pass1.append(
                MatchCandidate(
                    drug_info=candidate.drug_info,
                    score=breakdown.total,
                    method=candidate.method,
                    evidence=evidence,
                    validation_messages=candidate.validation_messages,
                )
            )

        pass1.sort(key=lambda c: c.score, reverse=True)

        # ── Pass 2: top 후보에 gap 보너스 적용 ──
        if len(pass1) >= 2:
            gap = pass1[0].score - pass1[1].score
            gap_ratio = min(gap / self.TOP_GAP_MAX, 1.0) if self.TOP_GAP_MAX > 0 else 0.0
            top_gap_bonus = round(gap_ratio * self.W_TOP_GAP, 3)
        elif len(pass1) == 1:
            top_gap_bonus = self.W_TOP_GAP  # 단독 후보
        else:
            top_gap_bonus = 0.0

        result: List[MatchCandidate] = []
        for idx, c in enumerate(pass1):
            evidence = dict(c.evidence)
            breakdown_dict = dict(evidence["score_breakdown"])

            if idx == 0:
                breakdown_dict["top_gap"] = top_gap_bonus
            else:
                breakdown_dict["top_gap"] = 0.0

            # total 재계산
            total = sum(
                v for k, v in breakdown_dict.items()
            )
            new_score = round(max(0.0, min(total, 1.0)), 3)

            evidence["score_breakdown"] = breakdown_dict

            result.append(
                MatchCandidate(
                    drug_info=c.drug_info,
                    score=new_score,
                    method=c.method,
                    evidence=evidence,
                    validation_messages=c.validation_messages,
                )
            )

        result.sort(key=lambda c: c.score, reverse=True)

        if result:
            top = result[0]
            logger.debug(
                f"PseudoConfidence top: {top.drug_info.item_name} "
                f"score={top.score:.3f}, breakdown={top.evidence.get('score_breakdown')}"
            )

        return result

    def _calculate_breakdown(
        self,
        candidate: MatchCandidate,
        normalized_drug: Optional[NormalizedDrug],
        top_gap: float = 0.0,
    ) -> ScoreBreakdown:
        """개별 후보의 점수 breakdown을 계산한다 (gap 보너스는 외부에서 설정)."""
        evidence = candidate.evidence
        raw_name_score = evidence.get("name_match", candidate.score)

        # 1) 이름 매칭 점수
        name_match = round(raw_name_score * self.W_NAME, 3)

        # 2) 용량 일치
        strength_val = evidence.get("strength_match")
        if strength_val is True:
            strength_match = self.W_STRENGTH
        elif strength_val is False:
            strength_match = 0.0  # penalty는 별도
        else:
            # 정보 없음 → 절반 배점
            strength_match = round(self.W_STRENGTH * 0.5, 3)

        # 3) 단위 일치 (용량이 일치하면 단위도 일치한 것으로 간주)
        if strength_val is True:
            unit_match = self.W_UNIT
        elif strength_val is False:
            unit_match = 0.0
        else:
            unit_match = round(self.W_UNIT * 0.5, 3)

        # 4) 제형 일치
        form_val = evidence.get("form_match")
        if form_val is True:
            form_match = self.W_FORM
        elif form_val is False:
            form_match = 0.0
        else:
            form_match = round(self.W_FORM * 0.5, 3)

        # 5) 제조사 일치
        mfr_val = evidence.get("manufacturer_match")
        if mfr_val is True:
            manufacturer_match = self.W_MANUFACTURER
        elif mfr_val is False:
            manufacturer_match = 0.0
        else:
            manufacturer_match = round(self.W_MANUFACTURER * 0.3, 3)

        # 6) 패널티
        penalty = 0.0
        if strength_val is False:
            penalty += self.PENALTY_STRENGTH_MISMATCH

        if normalized_drug and normalized_drug.ocr_confidence is not None:
            if normalized_drug.ocr_confidence < 0.75:
                penalty += self.PENALTY_LOW_OCR_CONFIDENCE

        penalty = round(penalty, 3)

        return ScoreBreakdown(
            name_match=name_match,
            strength_match=round(strength_match, 3),
            unit_match=round(unit_match, 3),
            form_match=round(form_match, 3),
            manufacturer_match=round(manufacturer_match, 3),
            top_gap=round(top_gap, 3),
            penalty=penalty,
        )
