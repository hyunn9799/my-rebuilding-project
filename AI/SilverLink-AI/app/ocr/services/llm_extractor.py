"""
LLM 기반 OCR 텍스트 구조화 추출 모듈 (Phase 6)

OCR 원문에서 약품명/용량/복용법 후보를 LLM으로 구조화 추출한다.
추출 결과는 hint 로만 사용되며, 최종 확정은 DB/Rule 검증이 담당한다.

설계 원칙:
  - LLM 결과만으로 MATCHED 확정 불가
  - LLM 실패/미사용 시 기존 deterministic pipeline 유지
  - hallucination 방지를 위해 "모르면 빈 배열 반환" 지시
"""
import json
import re
from typing import Any, Dict, List, Optional

from loguru import logger

from app.integration.llm.openai_client import LLM
from app.ocr.model.drug_model import NormalizedDrug


class LLMExtractor:
    """OCR 텍스트에서 약품명/용량/제형을 LLM으로 구조화 추출.

    추출 결과의 각 NormalizedDrug.source = 'llm_hint'로 설정되어,
    파이프라인의 안전 장치에서 LLM 단독 후보의 자동 확정을 방지한다.
    """

    def __init__(self, llm: LLM, enabled: bool = True):
        self.llm = llm
        self.enabled = enabled

    async def extract(self, ocr_text: str) -> List[NormalizedDrug]:
        """OCR 텍스트에서 구조화 약품 정보를 추출한다.

        Args:
            ocr_text: Luxia OCR 원문 텍스트

        Returns:
            NormalizedDrug 리스트 (source='llm_hint')
            실패 또는 비활성 시 빈 리스트 반환.
        """
        if not self.enabled:
            logger.debug("LLM extractor 비활성 상태")
            return []

        if not ocr_text or len(ocr_text.strip()) < 3:
            return []

        try:
            messages = self._build_prompt(ocr_text)

            completion = await self.llm.aclient.chat.completions.create(
                model=self.llm.model_version,
                messages=messages,
                temperature=0.0,
            )
            response_text = completion.choices[0].message.content
            logger.debug(f"LLM extraction 응답: {response_text[:300]}...")

            candidates = self._parse_response(response_text, ocr_text)
            logger.info(f"LLM extraction 완료: {len(candidates)}개 후보 추출")
            return candidates

        except Exception as e:
            logger.warning(f"LLM extraction 실패 (non-blocking): {e}")
            return []

    def _build_prompt(self, ocr_text: str) -> List[Dict[str, str]]:
        """LLM 구조화 추출 프롬프트 생성."""
        system_prompt = """당신은 약봉지 OCR 텍스트에서 약품 정보를 추출하는 전문가입니다.

**역할:**
OCR 텍스트에서 약품명, 용량, 제형을 구조화된 JSON으로 추출합니다.

**규칙:**
1. OCR 텍스트에 명시적으로 등장하는 약품명만 추출하세요.
2. 존재하지 않는 약품명을 만들어내지 마세요 (hallucination 금지).
3. 약품명이 확실하지 않으면 빈 배열 `[]`을 반환하세요.
4. 용량/제형이 불분명하면 null로 설정하세요.
5. 병원명, 환자명, 처방일, 복용 지시문(식전/식후 등)은 약품명이 아닙니다.
6. OCR 오류로 글자가 깨졌을 수 있으니, 합리적 범위 내에서 교정하세요.
   예: "타이레놀정 5 0 0mg" → name: "타이레놀정", dosage: "500mg"
   예: "아목시실린캡슐 5OOmg" → name: "아목시실린캡슐", dosage: "500mg"
7. 하나의 라인에 여러 약품이 있으면 각각 분리하세요.

**응답 형식 (JSON만 반환):**
```json
{
  "medications": [
    {
      "name": "약품명 (용량/제형 제외)",
      "dosage": "용량 (예: 500mg, 10ml) 또는 null",
      "form_type": "제형 (정, 캡슐, 시럽 등) 또는 null",
      "original_fragment": "이 약품이 추출된 원문 부분"
    }
  ]
}
```

약품을 하나도 찾지 못한 경우:
```json
{"medications": []}
```"""

        user_prompt = f"""다음 약봉지 OCR 텍스트에서 약품 정보를 추출해주세요.

=== OCR 텍스트 ===
{ocr_text}

JSON만 반환하세요."""

        return [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ]

    def _parse_response(
        self, response_text: str, ocr_text: str
    ) -> List[NormalizedDrug]:
        """LLM 응답을 NormalizedDrug 리스트로 변환."""
        try:
            # JSON 블록 추출
            json_match = re.search(
                r"```json\s*(.*?)\s*```", response_text, re.DOTALL
            )
            if json_match:
                json_str = json_match.group(1)
            else:
                json_str = response_text.strip()

            data = json.loads(json_str)
            medications = data.get("medications", [])

            if not isinstance(medications, list):
                logger.warning("LLM 응답의 medications가 배열이 아닙니다")
                return []

            results: List[NormalizedDrug] = []
            seen_names: set = set()

            for med in medications:
                if not isinstance(med, dict):
                    continue

                name = (med.get("name") or "").strip()
                if not name or len(name) < 2:
                    continue

                # 중복 방지
                name_key = name.lower()
                if name_key in seen_names:
                    continue
                seen_names.add(name_key)

                # 용량 정규화
                dosage = (med.get("dosage") or "").strip() or None
                form_type = (med.get("form_type") or "").strip() or None
                original = (med.get("original_fragment") or name).strip()

                # Hallucination guard: 추출된 이름이 OCR 원문에 일부라도 존재하는지 검증
                if not self._verify_in_source(name, ocr_text):
                    logger.debug(
                        f"LLM 추출 후보 '{name}'이 원문에 없어 제외 (hallucination 의심)"
                    )
                    continue

                results.append(
                    NormalizedDrug(
                        name=name,
                        dosage=dosage,
                        form_type=form_type,
                        original=original,
                        source="llm_hint",
                    )
                )

            return results

        except (json.JSONDecodeError, KeyError, TypeError) as e:
            logger.warning(f"LLM extraction 응답 파싱 실패: {e}")
            return []

    @staticmethod
    def _verify_in_source(name: str, ocr_text: str) -> bool:
        """추출된 이름이 OCR 원문에 존재하는지 검증 (hallucination 방지).

        완전 일치가 아니라, 이름의 핵심 부분(2글자 이상)이
        원문에 포함되어 있는지 확인한다.
        OCR 오류를 감안하여 느슨하게 검증.
        """
        ocr_lower = ocr_text.lower().replace(" ", "")
        name_lower = name.lower().replace(" ", "")

        # 전체 이름이 원문에 포함되면 OK
        if name_lower in ocr_lower:
            return True

        # 이름에서 한글 2글자 이상 연속 부분이 원문에 있으면 OK
        hangul_chunks = re.findall(r"[\uac00-\ud7a3]{2,}", name)
        for chunk in hangul_chunks:
            if chunk in ocr_text:
                return True

        # 이름에서 영문 3글자 이상 연속 부분이 원문에 있으면 OK
        latin_chunks = re.findall(r"[a-zA-Z]{3,}", name)
        for chunk in latin_chunks:
            if chunk.lower() in ocr_lower:
                return True

        return False
