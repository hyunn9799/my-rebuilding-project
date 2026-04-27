"""
OCR 텍스트 정규화 모듈
약봉지 OCR 텍스트에서 약품명 후보를 추출하고 정규화
"""
import re
from typing import List
from loguru import logger

from app.ocr.model.drug_model import NormalizedDrug


class TextNormalizer:
    """OCR 텍스트 → 정규화된 약품명 후보 리스트 변환"""

    # 제형 접미사 패턴
    FORM_TYPES = [
        "정", "캡슐", "시럽", "액", "산", "환", "크림", "연고",
        "주사", "패치", "과립", "현탁액", "점안액", "점이액",
        "좌제", "로션", "젤", "스프레이", "흡입제", "필름코팅정",
        "서방정", "츄어블정", "장용정", "구강붕해정",
    ]

    # 용량 패턴 (숫자 + 단위)
    DOSAGE_PATTERN = re.compile(
        r"(\d+(?:\.\d+)?)\s*(mg|ml|mcg|g|iu|밀리그램|밀리리터|마이크로그램|그램|단위|μg)",
        re.IGNORECASE,
    )

    # 약품명에서 제거할 노이즈 패턴
    NOISE_PATTERNS = [
        re.compile(r"[①②③④⑤⑥⑦⑧⑨⑩]"),  # 원문자
        re.compile(r"^\d+[\.\)]\s*"),  # 번호 (1. 2) 등)
        re.compile(r"[^\w가-힣a-zA-Z0-9\s\.\-]"),  # 특수문자 (일부 제외)
    ]

    # 한영 오인식 보정 매핑 (OCR에서 자주 틀리는 패턴)
    OCR_CORRECTIONS = {
        "타이레놀": ["타이레놀", "타이레놀정", "타이레늘"],
        "아스피린": ["아스파린", "아스피린"],
        "아모디핀": ["아모디핀", "아모다핀"],
    }

    def normalize(self, ocr_text: str) -> List[NormalizedDrug]:
        """
        OCR 텍스트에서 약품명 후보를 추출하고 정규화

        Args:
            ocr_text: OCR 원본 텍스트

        Returns:
            정규화된 약품명 후보 리스트
        """
        candidates = []
        lines = ocr_text.split("\n")

        for line in lines:
            line = line.strip()
            if not line or len(line) < 2:
                continue

            # 약품명 패턴이 없는 라인 스킵
            if not self._could_be_drug_name(line):
                continue

            # 정규화 처리
            normalized = self._normalize_line(line)
            if normalized:
                candidates.append(normalized)

        # 중복 제거
        seen = set()
        unique = []
        for c in candidates:
            if c.name not in seen:
                seen.add(c.name)
                unique.append(c)

        logger.debug(f"정규화 결과: {len(unique)}개 후보 — {[c.name for c in unique]}")
        return unique

    def _could_be_drug_name(self, line: str) -> bool:
        """약품명일 가능성이 있는 라인인지 판단"""
        # 한글 2글자 이상이 포함되어야 함
        korean_chars = re.findall(r"[가-힣]", line)
        if len(korean_chars) < 2:
            return False

        # 약품명 특성: 제형 접미사 또는 용량 단위 포함
        has_form = any(form in line for form in self.FORM_TYPES)
        has_dosage = bool(self.DOSAGE_PATTERN.search(line))
        has_korean_name = len(korean_chars) >= 2

        # 제형이나 용량이 있으면 높은 확률
        if has_form or has_dosage:
            return True

        # 한글만 있어도 약품명 후보로 간주 (길이 제한)
        if has_korean_name and len(line) <= 30:
            return True

        return False

    def _normalize_line(self, line: str) -> NormalizedDrug:
        """한 라인을 정규화"""
        original = line

        # 1. 노이즈 제거
        for pattern in self.NOISE_PATTERNS:
            line = pattern.sub("", line)
        line = line.strip()

        # 2. 용량 분리
        dosage = None
        dosage_match = self.DOSAGE_PATTERN.search(line)
        if dosage_match:
            dosage = dosage_match.group(0)
            # 약품명에서 용량 제거
            line_without_dosage = self.DOSAGE_PATTERN.sub("", line).strip()
        else:
            line_without_dosage = line

        # 3. 제형 분리
        form_type = None
        for form in sorted(self.FORM_TYPES, key=len, reverse=True):
            if line_without_dosage.endswith(form):
                form_type = form
                break

        # 4. 공백 정리
        name = re.sub(r"\s+", " ", line_without_dosage).strip()

        # 5. 빈 이름 방지
        if not name or len(name) < 2:
            name = re.sub(r"\s+", " ", original).strip()

        return NormalizedDrug(
            name=name,
            dosage=dosage,
            form_type=form_type,
            original=original,
        )

    def extract_dosage(self, text: str) -> str:
        """텍스트에서 용량 정보만 추출"""
        match = self.DOSAGE_PATTERN.search(text)
        return match.group(0) if match else ""

    def extract_form_type(self, text: str) -> str:
        """텍스트에서 제형 정보만 추출"""
        for form in sorted(self.FORM_TYPES, key=len, reverse=True):
            if form in text:
                return form
        return ""
