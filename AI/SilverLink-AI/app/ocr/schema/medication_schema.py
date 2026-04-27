from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import date


class OCRTokenInfo(BaseModel):
    """OCR 토큰별 신뢰도 정보"""
    value: str = Field(..., description="OCR 토큰 원문")
    confidence: Optional[float] = Field(None, ge=0.0, le=1.0, description="OCR 신뢰도")


class MedicationOCRRequest(BaseModel):
    """OCR 원본 텍스트 요청"""
    ocr_text: str = Field(..., description="Luxia OCR에서 추출된 원본 텍스트", alias="ocrText")
    elderly_user_id: Optional[int] = Field(None, description="어르신 사용자 ID", alias="elderlyUserId")
    ocr_tokens: List[OCRTokenInfo] = Field(
        default_factory=list,
        description="OCR 토큰별 confidence 정보",
        alias="ocrTokens",
    )
    
    class Config:
        populate_by_name = True  # 원래 이름과 alias 모두 허용


class MedicationInfo(BaseModel):
    """약 정보"""
    medication_name: str = Field(..., description="약 이름")
    dosage: Optional[str] = Field(None, description="용량 (예: 1정, 500mg)")
    times: List[str] = Field(default_factory=list, description="복용 시간 (morning, noon, evening, night)")
    instructions: Optional[str] = Field(None, description="복용 방법 (예: 식후 30분)")
    confidence: float = Field(..., description="신뢰도 (0.0 ~ 1.0)")
    category: Optional[str] = Field("기타", description="약 카테고리")
    # 신규 필드 (파이프라인)
    item_seq: Optional[str] = Field(None, description="품목기준코드 (e약은요)")
    entp_name: Optional[str] = Field(None, description="제약사명")
    match_score: Optional[float] = Field(None, description="DB 매칭 점수")
    match_method: Optional[str] = Field(None, description="매칭 방법 (exact/prefix/ngram/fuzzy/vector)")
    purpose: Optional[str] = Field(None, description="이 약의 용도 (쉬운 설명)")
    caution: Optional[str] = Field(None, description="주의사항 (쉬운 설명)")
    simple_name: Optional[str] = Field(None, description="쉬운 약 이름")
    evidence: dict = Field(default_factory=dict, description="매칭 및 검증 근거")
    validation_messages: List[str] = Field(default_factory=list, description="규칙 검증 메시지")


class PipelineStageInfo(BaseModel):
    """파이프라인 단계별 정보"""
    stage: str = Field(..., description="단계명")
    duration_ms: float = Field(..., description="소요시간(ms)")
    result_summary: str = Field("", description="결과 요약")


class MedicationOCRResponse(BaseModel):
    """OCR 검증 결과"""
    success: bool = Field(..., description="검증 성공 여부")
    medications: List[MedicationInfo] = Field(default_factory=list, description="추출된 약 정보 리스트")
    raw_ocr_text: str = Field(..., description="원본 OCR 텍스트")
    llm_analysis: str = Field("", description="LLM 분석 결과")
    warnings: List[str] = Field(default_factory=list, description="경고 메시지")
    error_message: Optional[str] = Field(None, description="에러 메시지")
    # 신규 필드 (파이프라인)
    pipeline_stages: List[PipelineStageInfo] = Field(default_factory=list, description="파이프라인 단계별 정보")
    total_duration_ms: Optional[float] = Field(None, description="전체 처리 시간(ms)")
    decision_status: str = Field("NOT_FOUND", description="최종 판정 상태")
    match_confidence: float = Field(0.0, description="최종 매칭 신뢰도")
    requires_user_confirmation: bool = Field(False, description="사용자 확인 필요 여부")
    decision_reasons: List[str] = Field(default_factory=list, description="최종 판정 사유")


class MedicationScheduleRequest(BaseModel):
    """복약 일정 등록 요청"""
    elderly_user_id: int = Field(..., description="어르신 사용자 ID")
    medication_name: str = Field(..., description="약 이름")
    dosage_text: Optional[str] = Field(None, description="용량")
    times: List[str] = Field(..., description="복용 시간")
    instructions: Optional[str] = Field(None, description="복용 방법")
    start_date: Optional[date] = Field(None, description="시작일")
    end_date: Optional[date] = Field(None, description="종료일")
    reminder: bool = Field(True, description="알림 여부")
