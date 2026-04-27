"""Rule-based candidate validation for medication OCR matching."""
import re
from decimal import Decimal, InvalidOperation
from typing import List, Optional

from loguru import logger

from app.ocr.model.drug_model import MatchCandidate, NormalizedDrug


class RuleValidator:
    """Re-rank candidates using high-risk medication entities."""

    UNIT_ALIASES = {
        "mg": "mg",
        "m.g": "mg",
        "\ubc00\ub9ac\uadf8\ub7a8": "mg",
        "\ubc00\ub9ac\uadf8\ub78c": "mg",
        "ml": "ml",
        "\ubc00\ub9ac\ub9ac\ud130": "ml",
        "\ubc00\ub9ac\ub9ac\ud2b8": "ml",
        "mcg": "mcg",
        "\u03bcg": "mcg",
        "\ub9c8\uc774\ud06c\ub85c\uadf8\ub7a8": "mcg",
        "\ub9c8\uc774\ud06c\ub85c\uadf8\ub78c": "mcg",
        "g": "g",
        "\uadf8\ub7a8": "g",
        "\uadf8\ub78c": "g",
        "iu": "iu",
        "\ub2e8\uc704": "iu",
    }
    UNIT_PATTERN = (
        r"mg|m\.g|ml|mcg|\u03bcg|g|iu|"
        r"\ubc00\ub9ac\uadf8\ub7a8|\ubc00\ub9ac\uadf8\ub78c|"
        r"\ubc00\ub9ac\ub9ac\ud130|\ubc00\ub9ac\ub9ac\ud2b8|"
        r"\ub9c8\uc774\ud06c\ub85c\uadf8\ub7a8|\ub9c8\uc774\ud06c\ub85c\uadf8\ub78c|"
        r"\uadf8\ub7a8|\uadf8\ub78c|\ub2e8\uc704"
    )
    STRENGTH_PATTERN = re.compile(
        rf"(?P<value>\d+(?:\.\d+)?)\s*(?P<unit>{UNIT_PATTERN})(?![a-zA-Z])",
        re.IGNORECASE,
    )
    KEYWORDS = [
        "\ud574\uc5f4",
        "\uc9c4\ud1b5",
        "\uac10\uae30",
        "\uace0\ud608\uc555",
        "\ud608\ub2f9",
        "\uc704\uc7a5",
        "\uc18c\ud654",
        "\ubd88\uba74",
        "\uc54c\ub808\ub974\uae30",
        "\ucc9c\uc2dd",
    ]

    def validate(
        self,
        candidates: List[MatchCandidate],
        ocr_text: str,
        normalized_drug: Optional[NormalizedDrug] = None,
    ) -> List[MatchCandidate]:
        if not candidates:
            return candidates

        ocr_lower = ocr_text.lower()
        validated: list[MatchCandidate] = []

        for candidate in candidates:
            bonus = 0.0
            penalty = 0.0
            drug = candidate.drug_info
            evidence = dict(candidate.evidence)
            messages = list(candidate.validation_messages)

            if drug.entp_name and drug.entp_name in ocr_text:
                bonus += 0.04
                evidence["manufacturer_match"] = True
                messages.append(f"manufacturer matched: {drug.entp_name}")
            elif drug.entp_name:
                evidence.setdefault("manufacturer_match", None)

            if normalized_drug and normalized_drug.dosage:
                strength_result = self._compare_strength(normalized_drug.dosage, drug.item_name)
                evidence["ocr_strength"] = normalized_drug.dosage

                if strength_result is True:
                    bonus += 0.12
                    evidence["strength_match"] = True
                    messages.append(f"strength matched: {normalized_drug.dosage}")
                elif strength_result is False:
                    penalty += 0.35
                    evidence["strength_match"] = False
                    messages.append(f"strength mismatch: OCR={normalized_drug.dosage}, DB={drug.item_name}")
                else:
                    evidence["strength_match"] = None

            if normalized_drug and normalized_drug.form_type:
                if normalized_drug.form_type.lower() in drug.item_name.lower():
                    bonus += 0.03
                    evidence["form_match"] = True
                    messages.append(f"form matched: {normalized_drug.form_type}")
                else:
                    evidence["form_match"] = False

            if normalized_drug and normalized_drug.ocr_confidence is not None:
                evidence["ocr_confidence"] = normalized_drug.ocr_confidence
                if normalized_drug.ocr_confidence < 0.75:
                    penalty += 0.08
                    messages.append(f"low OCR confidence: {normalized_drug.ocr_confidence}")

            keyword_bonus = self._keyword_bonus(drug.efcy_qesitm or "", ocr_lower)
            if keyword_bonus:
                bonus += keyword_bonus
                evidence["keyword_bonus"] = round(keyword_bonus, 3)

            new_score = max(0.0, min(candidate.score + bonus - penalty, 1.0))
            evidence["rule_bonus"] = round(bonus, 3)
            evidence["rule_penalty"] = round(penalty, 3)

            validated.append(
                MatchCandidate(
                    drug_info=drug,
                    score=round(new_score, 3),
                    method=candidate.method,
                    evidence=evidence,
                    validation_messages=messages,
                )
            )

        validated.sort(key=lambda item: item.score, reverse=True)
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
