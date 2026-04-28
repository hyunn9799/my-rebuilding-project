"""OCR 파이프라인 결과 저장 모델."""
from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class OcrResultRecord(BaseModel):
    """OCR 파이프라인 결과 저장 레코드.

    medication_ocr_results 테이블의 Python 표현.
    JSON 컬럼(candidates, normalized_names 등)은 dict/list로 직렬화하여 저장.
    """

    id: Optional[int] = None
    request_id: str = Field(..., description="UUID, 요청별 고유 ID")
    elderly_user_id: Optional[int] = Field(None, description="어르신 사용자 ID")

    # OCR 원문
    raw_ocr_text: str = Field(..., description="Luxia OCR 원문")

    # 파이프라인 결과 (JSON 직렬화)
    normalized_names: List[Dict[str, Any]] = Field(
        default_factory=list, description="정규화된 약품명 후보 배열"
    )
    candidates: List[Dict[str, Any]] = Field(
        default_factory=list, description="매칭 후보 전체 (score, method, evidence 포함)"
    )
    pipeline_stages: List[Dict[str, Any]] = Field(
        default_factory=list, description="파이프라인 단계별 소요시간"
    )

    # 최종 판정
    decision_status: str = Field("NOT_FOUND", description="최종 판정 상태")
    match_confidence: float = Field(0.0, ge=0.0, le=1.0, description="최종 매칭 신뢰도")
    decision_reasons: List[str] = Field(default_factory=list, description="판정 사유 배열")
    best_drug_item_seq: Optional[str] = Field(None, description="최고 후보 item_seq")
    best_drug_name: Optional[str] = Field(None, description="최고 후보 약품명")

    # 사용자 확인 결과
    user_confirmed: Optional[bool] = Field(
        None, description="None=미확인, True=확정, False=거부"
    )
    user_selected_seq: Optional[str] = Field(
        None, description="사용자가 선택한 item_seq"
    )
    user_confirmed_at: Optional[datetime] = Field(
        None, description="사용자 확인 시점"
    )

    # LLM 결과
    llm_description: Optional[str] = Field(None, description="LLM 생성 설명")
    warnings: List[str] = Field(default_factory=list, description="경고 메시지 배열")

    # 메타
    total_duration_ms: Optional[float] = Field(None, description="전체 처리 시간(ms)")
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None
