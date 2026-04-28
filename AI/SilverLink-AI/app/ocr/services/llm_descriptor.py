"""
LLM 기반 약품 설명 생성 모듈
파이프라인 최종 단계 — DB에서 확정된 약품 정보를 어르신 맞춤 설명으로 변환
"""
import json
import re
from typing import Dict, Any, List
from loguru import logger

from app.integration.llm.openai_client import LLM
from app.ocr.model.drug_model import MatchCandidate


class LLMDescriptor:
    """확정된 약품 정보를 기반으로 어르신 맞춤 설명 생성"""

    def __init__(self, llm: LLM):
        self.llm = llm

    async def generate_description(
        self,
        candidates: List[MatchCandidate],
        ocr_text: str,
        decision_status: str = "MATCHED",
    ) -> Dict[str, Any]:
        """
        확정된 약품 정보 + OCR 원문을 LLM에 전달하여 설명 생성

        Args:
            candidates: 규칙 검증까지 완료된 매칭 후보
            ocr_text: OCR 원문 텍스트

        Returns:
            {
                "description": "어르신 맞춤 설명",
                "medications": [약품별 상세 정보],
                "warnings": [주의사항]
            }
        """
        if not candidates:
            return {
                "description": "약 정보를 찾을 수 없습니다.",
                "medications": [],
                "warnings": ["OCR 텍스트에서 약품을 식별하지 못했습니다."],
            }

        try:
            messages = self._build_prompt(candidates, ocr_text, decision_status)

            # 비동기 LLM 호출
            completion = await self.llm.aclient.chat.completions.create(
                model=self.llm.model_version,
                messages=messages,
            )
            response = completion.choices[0].message.content
            logger.debug(f"LLM 설명 생성 응답: {response[:200]}...")

            return self._parse_response(response, candidates)

        except Exception as e:
            logger.error(f"LLM 설명 생성 실패: {e}")
            # fallback: DB 정보만으로 기본 설명 생성
            return self._fallback_description(candidates)

    def _build_prompt(
        self,
        candidates: List[MatchCandidate],
        ocr_text: str,
        decision_status: str,
    ) -> list:
        """LLM 프롬프트 생성"""
        # 약품 정보를 컨텍스트로 구성
        drug_context = []
        for i, c in enumerate(candidates[:5], 1):
            drug = c.drug_info
            info = f"""
약품 {i}:
- 약품명: {drug.item_name}
- 업체: {drug.entp_name or '정보 없음'}
- 성분: {drug.item_ingr_name or '정보 없음'}
- 전문/일반: {drug.spclty_pblc or '정보 없음'}
- 효능효과: {(drug.efcy_qesitm or '정보 없음')[:300]}
- 사용법: {(drug.use_method_qesitm or '정보 없음')[:300]}
- 주의사항: {(drug.atpn_qesitm or '정보 없음')[:300]}
- 매칭 신뢰도: {c.score}
- 매칭 방법: {c.method}
- 검증 근거: {json.dumps(c.evidence, ensure_ascii=False)}
"""
            drug_context.append(info)

        system_prompt = """당신은 어르신(65세 이상)을 위한 복약 안내 도우미입니다.
아래 검증된 약품 정보만 바탕으로, 어르신이 쉽게 이해할 수 있도록 설명해주세요.

**규칙:**
1. 존댓말 사용 (해요체)
2. 의학 용어를 쉬운 말로 풀어서 설명
3. 복용 시간과 방법을 명확하게 안내
4. 주의사항은 꼭 포함
5. JSON 형식으로 응답
6. 제공된 DB 정보에 없는 효능, 부작용, 복용법을 새로 만들지 않기
7. 약품 식별, 처방 변경, 복용 중단 판단을 하지 않기
8. MATCHED가 아닌 상태에서는 "확정"이라고 말하지 말고 사용자 확인이 필요하다고 안내하기

**응답 형식:**
```json
{
    "description": "전체 요약 설명 (2-3문장, 어르신이 이해하기 쉽게)",
    "medications": [
        {
            "medication_name": "약품명",
            "simple_name": "쉬운 약 이름",
            "dosage": "용량",
            "times": ["morning", "noon", "evening"],
            "instructions": "복용법 (쉽게)",
            "purpose": "이 약은 ~을 위한 약이에요",
            "caution": "주의사항 (쉽게)"
        }
    ],
    "warnings": ["전체 주의사항"]
}
```

복용 시간 매핑:
- 아침, 조식, morning → "morning"
- 점심, 중식, noon → "noon"
- 저녁, 석식, evening → "evening"
- 취침전, 자기전, night → "night"
"""

        user_prompt = f"""다음은 약봉지 OCR 텍스트와 DB에서 식별된 약품 정보입니다.

=== 판정 상태 ===
{decision_status}

=== OCR 원문 ===
{ocr_text}

=== 식별된 약품 정보 ===
{"".join(drug_context)}

위 정보를 바탕으로 어르신에게 쉽게 설명해주세요.
"""

        return [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ]

    def _parse_response(self, response: str, candidates: List[MatchCandidate]) -> Dict[str, Any]:
        """LLM 응답 파싱"""
        try:
            json_match = re.search(r"```json\s*(.*?)\s*```", response, re.DOTALL)
            if json_match:
                json_str = json_match.group(1)
            else:
                json_str = response.strip()

            data = json.loads(json_str)

            # 기본 필드 보장
            return {
                "description": data.get("description", ""),
                "medications": data.get("medications", []),
                "warnings": data.get("warnings", []),
            }
        except json.JSONDecodeError:
            logger.warning("LLM 응답 JSON 파싱 실패, fallback 사용")
            return self._fallback_description(candidates)

    def _fallback_description(self, candidates: List[MatchCandidate]) -> Dict[str, Any]:
        """LLM 실패 시 DB 정보만으로 기본 설명 생성"""
        medications = []
        for c in candidates[:5]:
            drug = c.drug_info
            medications.append({
                "medication_name": drug.item_name,
                "simple_name": drug.item_name,
                "dosage": None,
                "times": ["morning", "evening"],
                "instructions": drug.use_method_qesitm[:200] if drug.use_method_qesitm else None,
                "purpose": (
                    drug.efcy_qesitm[:200] if drug.efcy_qesitm
                    else (f"성분: {drug.item_ingr_name}" if drug.item_ingr_name else None)
                ),
                "caution": drug.atpn_qesitm[:200] if drug.atpn_qesitm else None,
            })

        return {
            "description": f"{len(medications)}개의 약이 확인되었어요. 상세 정보를 확인해주세요.",
            "medications": medications,
            "warnings": ["AI 분석이 완료되지 않았어요. 약사님이나 의사 선생님께 확인해주세요."],
        }
