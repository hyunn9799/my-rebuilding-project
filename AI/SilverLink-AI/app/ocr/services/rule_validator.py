"""
규칙 기반 약품 후보 검증 모듈
성분/함량/업체명/제형을 교차 검증하여 후보 리랭킹
"""
import re
from typing import List, Optional
from loguru import logger

from app.ocr.model.drug_model import MatchCandidate, NormalizedDrug


class RuleValidator:
    """규칙 기반 약품 후보 검증 및 리랭킹"""

    # 함량 단위 정규화 맵
    DOSAGE_UNIT_MAP = {
        "mg": "밀리그램",
        "ml": "밀리리터",
        "mcg": "마이크로그램",
        "μg": "마이크로그램",
        "g": "그램",
        "iu": "단위",
    }

    def validate(
        self,
        candidates: List[MatchCandidate],
        ocr_text: str,
        normalized_drug: Optional[NormalizedDrug] = None,
    ) -> List[MatchCandidate]:
        """
        규칙 기반 검증으로 후보 리랭킹

        Args:
            candidates: 매칭 후보 리스트
            ocr_text: 원본 OCR 텍스트
            normalized_drug: 정규화된 약품 정보 (용량, 제형 등)

        Returns:
            리랭킹된 후보 리스트
        """
        if not candidates:
            return candidates

        ocr_lower = ocr_text.lower()
        validated = []

        for candidate in candidates:
            bonus = 0.0
            drug = candidate.drug_info

            # 규칙 1: 업체명 매칭
            if drug.entp_name and drug.entp_name in ocr_text:
                bonus += 0.05
                logger.debug(f"규칙검증 — 업체명 일치: {drug.entp_name}")

            # 규칙 2: 함량 매칭
            if normalized_drug and normalized_drug.dosage:
                if self._dosage_matches(normalized_drug.dosage, drug.item_name):
                    bonus += 0.05
                    logger.debug(f"규칙검증 — 함량 일치: {normalized_drug.dosage}")

            # 규칙 3: 제형 매칭
            if normalized_drug and normalized_drug.form_type:
                if normalized_drug.form_type in drug.item_name:
                    bonus += 0.03
                    logger.debug(f"규칙검증 — 제형 일치: {normalized_drug.form_type}")

            # 규칙 4: 효능효과 키워드 매칭
            if drug.efcy_qesitm:
                keyword_bonus = self._keyword_bonus(drug.efcy_qesitm, ocr_lower)
                bonus += keyword_bonus

            # 보너스 적용 (최대 1.0)
            new_score = min(candidate.score + bonus, 1.0)

            validated.append(
                MatchCandidate(
                    drug_info=drug,
                    score=round(new_score, 3),
                    method=candidate.method,
                )
            )

        # 점수 내림차순 정렬
        validated.sort(key=lambda c: c.score, reverse=True)
        return validated

    def _dosage_matches(self, ocr_dosage: str, item_name: str) -> bool:
        """OCR 용량과 DB 약품명의 용량 비교"""
        # OCR 용량에서 숫자 추출
        ocr_numbers = re.findall(r"(\d+(?:\.\d+)?)", ocr_dosage)
        if not ocr_numbers:
            return False

        ocr_num = ocr_numbers[0]

        # DB 약품명에서도 숫자 추출
        item_numbers = re.findall(r"(\d+(?:\.\d+)?)", item_name)

        # 같은 숫자가 있으면 일치
        return ocr_num in item_numbers

    def _keyword_bonus(self, efcy_text: str, ocr_lower: str) -> float:
        """효능효과 텍스트와 OCR 텍스트 간 키워드 매칭 보너스"""
        # 대표적 질환/증상 키워드
        keywords = [
            "혈압", "고혈압", "당뇨", "혈당", "통증", "진통", "해열",
            "감기", "소화", "위장", "수면", "불면", "비타민",
            "콜레스테롤", "항생제", "알레르기", "천식",
        ]

        matched = 0
        for kw in keywords:
            if kw in efcy_text and kw in ocr_lower:
                matched += 1

        if matched > 0:
            return min(matched * 0.02, 0.05)
        return 0.0
