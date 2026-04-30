"""Medication OCR text normalization."""
import re
import unicodedata
from typing import List, Optional

from loguru import logger

from app.ocr.model.drug_model import NormalizedDrug, OCRToken


class TextNormalizer:
    """Extract normalized medication-name candidates from raw OCR text."""

    FORM_TYPES = [
        "\uc815",  # tablet
        "\uc815\uc81c",
        "\ucea1\uc290",
        "\ucea1\uc290\uc81c",
        "\uc2dc\ub7fd",
        "\uc2dc\ub7fd\uc81c",
        "\ud604\ud0c1\uc561",
        "\uc561",
        "\uc8fc\uc0ac",
        "\uc8fc\uc0ac\uc81c",
        "\ud06c\ub9bc",
        "\uc5f0\uace0",
        "\uc88c\uc81c",
        "\ub85c\uc158",
        "\ud328\uce58",
        "\uc2a4\ud504\ub808\uc774",
        "\ud761\uc785",
        "\uad6c\uac15\ubd95\ud574\uc815",
        "\uc11c\ubc29\uc815",
        "\uc7a5\uc6a9\uc815",
        "tablet",
        "tab",
        "capsule",
        "cap",
        "syrup",
        "injection",
    ]

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
    DOSAGE_PATTERN = re.compile(
        rf"(?P<value>\d+(?:\.\d+)?)\s*(?P<unit>{UNIT_PATTERN})(?![a-zA-Z])",
        re.IGNORECASE,
    )
    SPACED_DIGIT_DOSAGE_PATTERN = re.compile(
        rf"(?<!\d)(?P<value>\d(?:\s+\d){{1,5}})\s*(?P<unit>{UNIT_PATTERN})(?![a-zA-Z])",
        re.IGNORECASE,
    )
    SPACED_OCR_ZERO_DOSAGE_PATTERN = re.compile(
        rf"(?<!\d)(?P<value>\d(?:\s+[\doO]){{1,5}})\s*(?P<unit>{UNIT_PATTERN})(?![a-zA-Z])",
        re.IGNORECASE,
    )
    OCR_ZERO_DOSAGE_PATTERN = re.compile(
        rf"(?P<value>\d[\doO]{{1,5}})\s*(?P<unit>{UNIT_PATTERN})(?![a-zA-Z])",
        re.IGNORECASE,
    )

    ADMIN_KEYWORDS = [
        "\ud658\uc790",
        "\ubcd1\uc6d0",
        "\ucc98\ubc29",
        "\uc870\uc81c",
        "\uc758\uc0ac",
        "\uc57d\uc0ac",
        "\uad50\ubd80",
        "\ubc88\ud638",
        "\uc8fc\uc758\uc0ac\ud56d",
    ]
    DOSING_KEYWORDS = [
        "\uc2dd\uc804",
        "\uc2dd\ud6c4",
        "\uc544\uce68",
        "\uc810\uc2ec",
        "\uc800\ub141",
        "\ucde8\uce68",
        "\ud558\ub8e8",
        "\ubcf5\uc6a9",
        "\ud68c",
        "\uc77c",
        "\ud3ec",
        "\uc815\uc529",
    ]

    NOISE_PATTERN = re.compile(r"[^\w\uac00-\ud7a3a-zA-Z0-9\s.\-/%()]")
    LEADING_NUMBER_PATTERN = re.compile(r"^\s*\d+[\.\)]\s*")

    def normalize(
        self,
        ocr_text: str,
        ocr_tokens: Optional[List[OCRToken]] = None,
    ) -> List[NormalizedDrug]:
        candidates: List[NormalizedDrug] = []
        token_confidence = self._build_token_confidence_map(ocr_tokens or [])

        for raw_line in ocr_text.splitlines():
            line = raw_line.strip()
            if not line or len(line) < 2:
                continue

            normalized_text = self.normalize_text(line)
            if not self._could_be_drug_name(normalized_text):
                continue

            normalized = self._normalize_line(normalized_text, original=line)
            if normalized:
                normalized.ocr_confidence = self._line_confidence(line, token_confidence)
                candidates.append(normalized)

        unique: list[NormalizedDrug] = []
        seen: set[tuple[str, Optional[str]]] = set()
        for candidate in candidates:
            key = (candidate.name, candidate.dosage)
            if key in seen:
                continue
            seen.add(key)
            unique.append(candidate)

        logger.debug("Text normalization produced {} candidate(s): {}", len(unique), [c.name for c in unique])
        return unique

    def normalize_text(self, text: str) -> str:
        text = unicodedata.normalize("NFKC", text)
        text = text.replace("\u00b5", "\u03bc")
        text = self.SPACED_OCR_ZERO_DOSAGE_PATTERN.sub(self._join_spaced_ocr_zero_digits, text)
        text = self.SPACED_DIGIT_DOSAGE_PATTERN.sub(self._join_spaced_digits, text)
        text = self.OCR_ZERO_DOSAGE_PATTERN.sub(self._fix_ocr_zero_digits, text)
        text = self.NOISE_PATTERN.sub(" ", text)
        text = re.sub(r"\s+", " ", text).strip()
        return self._normalize_units(text)

    def _normalize_units(self, text: str) -> str:
        def replace(match: re.Match) -> str:
            value = match.group("value")
            unit = self.UNIT_ALIASES.get(match.group("unit").lower(), match.group("unit").lower())
            return f"{value}{unit}"

        return self.DOSAGE_PATTERN.sub(replace, text)

    def _join_spaced_digits(self, match: re.Match) -> str:
        value = re.sub(r"\s+", "", match.group("value"))
        return f"{value}{match.group('unit')}"

    def _join_spaced_ocr_zero_digits(self, match: re.Match) -> str:
        value = re.sub(r"\s+", "", match.group("value"))
        value = value.replace("O", "0").replace("o", "0")
        return f"{value}{match.group('unit')}"

    def _fix_ocr_zero_digits(self, match: re.Match) -> str:
        value = match.group("value").replace("O", "0").replace("o", "0")
        return f"{value}{match.group('unit')}"

    def _could_be_drug_name(self, line: str) -> bool:
        if len(line) > 80:
            return False

        lower_line = line.lower()
        has_hangul = bool(re.search(r"[\uac00-\ud7a3]{2,}", line))
        has_latin_name = bool(re.search(r"[a-zA-Z]{3,}", line))
        has_dosage = bool(self.DOSAGE_PATTERN.search(line))
        has_form = any(form.lower() in lower_line for form in self.FORM_TYPES)

        if not (has_hangul or has_latin_name):
            return False

        if any(keyword in line for keyword in self.ADMIN_KEYWORDS):
            return False

        if not (has_dosage or has_form) and any(keyword in line for keyword in self.DOSING_KEYWORDS):
            return False

        return has_dosage or has_form or len(line) <= 40

    def _normalize_line(self, line: str, original: str) -> Optional[NormalizedDrug]:
        line = self.LEADING_NUMBER_PATTERN.sub("", line).strip()
        if not line:
            return None

        dosage = self.extract_dosage(line) or None
        line_without_dosage = self.DOSAGE_PATTERN.sub(" ", line).strip()

        form_type = self.extract_form_type(line_without_dosage) or None
        name = re.sub(r"\s+", " ", line_without_dosage).strip(" -/%()")

        if not name or len(name) < 2:
            name = re.sub(r"\s+", " ", line).strip(" -/%()")

        if not name or len(name) < 2:
            return None

        return NormalizedDrug(
            name=name,
            dosage=dosage,
            form_type=form_type,
            original=original,
        )

    def _build_token_confidence_map(self, tokens: List[OCRToken]) -> dict[str, float]:
        values: dict[str, list[float]] = {}
        for token in tokens:
            if token.confidence is None:
                continue
            key = self.normalize_text(token.value).lower()
            if not key:
                continue
            values.setdefault(key, []).append(token.confidence)
        return {key: sum(items) / len(items) for key, items in values.items()}

    def _line_confidence(self, line: str, token_confidence: dict[str, float]) -> Optional[float]:
        if not token_confidence:
            return None

        normalized_line = self.normalize_text(line).lower()
        matched = [
            confidence
            for token, confidence in token_confidence.items()
            if token and token in normalized_line
        ]
        if not matched:
            return None
        return round(sum(matched) / len(matched), 3)

    def extract_dosage(self, text: str) -> str:
        normalized_text = self._normalize_units(text)
        match = self.DOSAGE_PATTERN.search(normalized_text)
        if not match:
            return ""
        unit = self.UNIT_ALIASES.get(match.group("unit").lower(), match.group("unit").lower())
        return f"{match.group('value')}{unit}"

    def extract_form_type(self, text: str) -> str:
        lower_text = text.lower()
        for form in sorted(self.FORM_TYPES, key=len, reverse=True):
            if form.lower() in lower_text:
                return form
        return ""
