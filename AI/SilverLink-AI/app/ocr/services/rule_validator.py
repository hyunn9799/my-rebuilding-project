"""Rule-based candidate validation for medication OCR matching.

RuleValidator는 evidence 태깅과 검증 메시지 생성만 담당한다.
점수 재계산은 PseudoConfidenceScorer가 수행한다.
"""
import re
from decimal import Decimal, InvalidOperation
from typing import List, Optional

from loguru import logger

from app.ocr.model.drug_model import MatchCandidate, NormalizedDrug


class RuleValidator:
    """Evidence 태깅 및 규칙 검증 메시지 생성."""

    UNIT_ALIASES = {
        "mg": "mg",
        "m.g": "mg",
        "밀리그램": "mg",
        "밀리그람": "mg",
        "ml": "ml",
        "밀리리터": "ml",
        "밀리리트": "ml",
        "mcg": "mcg",
        "μg": "mcg",
        "마이크로그램": "mcg",
        "마이크로그람": "mcg",
        "g": "g",
        "그램": "g",
        "그람": "g",
        "iu": "iu",
        "단위": "iu",
    }
    UNIT_PATTERN = (
        r"mg|m\.g|ml|mcg|μg|g|iu|"
        r"밀리그램|밀리그람|"
        r"밀리리터|밀리리트|"
        r"마이크로그램|마이크로그람|"
        r"그램|그람|단위"
    )
    STRENGTH_PATTERN = re.compile(
        rf"(?P<value>\d+(?:\.\d+)?)\s*(?P<unit>{UNIT_PATTERN})(?![a-zA-Z])",
        re.IGNORECASE,
    )
    KEYWORDS = [
        "해열",
        "진통",
        "감기",
        "고혈압",
        "혈당",
        "위장",
        "소화",
        "불면",
        "알레르기",
        "천식",
    ]

    def validate(
        self,
        candidates: List[MatchCandidate],
        ocr_text: str,
        normalized_drug: Optional[NormalizedDrug] = None,
    ) -> List[MatchCandidate]:
        """evidence 태깅만 수행. 점수는 변경하지 않는다."""
        if not candidates:
            return candidates

        ocr_lower = ocr_text.lower()
        validated: list[MatchCandidate] = []

        for candidate in candidates:
            drug = candidate.drug_info
            evidence = dict(candidate.evidence)
            messages = list(candidate.validation_messages)

            # 제조사 일치
            if drug.entp_name and drug.entp_name in ocr_text:
                evidence["manufacturer_match"] = True
                messages.append(f"manufacturer matched: {drug.entp_name}")
            elif drug.entp_name:
                evidence.setdefault("manufacturer_match", None)

            # 용량 일치
            if normalized_drug and normalized_drug.dosage:
                strength_result = self._compare_strength(normalized_drug.dosage, drug.item_name)
                evidence["ocr_strength"] = normalized_drug.dosage

                if strength_result is True:
                    evidence["strength_match"] = True
                    messages.append(f"strength matched: {normalized_drug.dosage}")
                elif strength_result is False:
                    evidence["strength_match"] = False
                    messages.append(f"strength mismatch: OCR={normalized_drug.dosage}, DB={drug.item_name}")
                else:
                    evidence["strength_match"] = None

            # 제형 일치
            if normalized_drug and normalized_drug.form_type:
                if normalized_drug.form_type.lower() in drug.item_name.lower():
                    evidence["form_match"] = True
                    messages.append(f"form matched: {normalized_drug.form_type}")
                else:
                    evidence["form_match"] = False

            # OCR 신뢰도
            if normalized_drug and normalized_drug.ocr_confidence is not None:
                evidence["ocr_confidence"] = normalized_drug.ocr_confidence
                if normalized_drug.ocr_confidence < 0.75:
                    messages.append(f"low OCR confidence: {normalized_drug.ocr_confidence}")

            # 키워드 매칭
            keyword_bonus = self._keyword_bonus(drug.efcy_qesitm or "", ocr_lower)
            if keyword_bonus:
                evidence["keyword_bonus"] = round(keyword_bonus, 3)

            validated.append(
                MatchCandidate(
                    drug_info=drug,
                    score=candidate.score,  # 점수 변경 없음
                    method=candidate.method,
                    evidence=evidence,
                    validation_messages=messages,
                )
            )

        return validated

    def _compare_strength(self, ocr_dosage: str, item_name: str) -> Optional[bool]:
        ocr_strengths = self._extract_strengths(ocr_dosage)
        item_strengths = self._extract_strengths(item_name)

        if not ocr_strengths:
            return None
        if not item_strengths:
            return None

        for ocr_value, ocr_unit in ocr_strengths:
            for item_value, item_unit in item_strengths:
                if ocr_value == item_value and ocr_unit == item_unit:
                    return True

        return False

    def _extract_strengths(self, text: str) -> list[tuple[Decimal, str]]:
        strengths: list[tuple[Decimal, str]] = []
        for match in self.STRENGTH_PATTERN.finditer(text):
            try:
                value = Decimal(match.group("value")).normalize()
            except InvalidOperation:
                continue
            unit = self.UNIT_ALIASES.get(match.group("unit").lower(), match.group("unit").lower())
            strengths.append((value, unit))
        return strengths

    def _keyword_bonus(self, efcy_text: str, ocr_lower: str) -> float:
        if not efcy_text:
            return 0.0

        matched = 0
        for keyword in self.KEYWORDS:
            if keyword in efcy_text and keyword in ocr_lower:
                matched += 1

        if matched:
            logger.debug("keyword validation matched {} keyword(s)", matched)
            return min(matched * 0.02, 0.05)
        return 0.0
