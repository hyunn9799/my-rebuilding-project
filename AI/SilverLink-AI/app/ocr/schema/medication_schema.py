from pydantic import BaseModel, Field
from typing import Any, List, Optional
from datetime import date, datetime


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
    request_id: Optional[str] = Field(None, description="결과 추적용 UUID")


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


class ConfirmMedicationRequest(BaseModel):
    """사용자 약품 후보 확인 요청"""
    request_id: str = Field(..., description="OCR 결과 request_id (UUID)", alias="requestId")
    selected_item_seq: str = Field(..., description="사용자가 선택한 item_seq", alias="selectedItemSeq")
    confirmed: bool = Field(True, description="True=확정, False=거부")

    class Config:
        populate_by_name = True


class ConfirmMedicationResponse(BaseModel):
    """사용자 약품 후보 확인 응답"""
    success: bool = Field(..., description="처리 성공 여부")
    message: str = Field("", description="결과 메시지")
    alias_suggestion_created: bool = Field(False, description="alias 제안 생성 여부")


class OcrResultOwnerResponse(BaseModel):
    """requestId 소유자 조회 응답"""
    request_id: str = Field(..., description="OCR 결과 request_id")
    elderly_user_id: int = Field(..., description="OCR 결과 소유 어르신 사용자 ID")
    decision_status: str = Field(..., description="OCR 결과 판정 상태")
    user_confirmed: Optional[bool] = Field(None, description="None=미확인, True=확정, False=거부")


class VectorStatusResponse(BaseModel):
    """ChromaDB vector fallback 상태 응답"""
    collection_name: str = Field(..., description="ChromaDB collection name")
    persist_directory: str = Field(..., description="ChromaDB persist directory")
    count: Optional[int] = Field(None, description="현재 vector count")
    expected_count: Optional[int] = Field(None, description="기대 vector count")
    embedding_model: Optional[str] = Field(None, description="Embedding model name")
    status: str = Field(..., description="READY, EMPTY, COUNT_MISMATCH, ERROR")
    message: str = Field(..., description="상태 설명")
    is_degraded: bool = Field(..., description="검색 품질 저하 가능 여부")
    checked_at: datetime = Field(..., description="상태 확인 시각")


class QualityReportRunRequest(BaseModel):
    """관리자 OCR 품질 리포트 실행 요청"""
    limit: int = Field(20, ge=1, le=200, description="분석할 샘플 수")
    include_candidates: bool = Field(True, description="alias 후보 목록 포함 여부")
    persist_files: bool = Field(False, description="서버 로컬 docs 파일 저장 여부")


class QualityReportRunResponse(BaseModel):
    """관리자 OCR 품질 리포트 실행 응답"""
    success: bool = Field(..., description="실행 성공 여부")
    generated_at: str = Field(..., description="리포트 생성 시각")
    decision_counts: List[dict] = Field(default_factory=list)
    suggestion_counts: List[dict] = Field(default_factory=list)
    match_method_counts: List[Any] = Field(default_factory=list)
    recommended_action_counts: dict = Field(default_factory=dict)
    alias_candidate_count: int = 0
    manual_review_count: int = 0
    normalization_candidate_count: int = 0
    report_markdown: str = ""
    alias_candidates: List[dict] = Field(default_factory=list)
    message: str = ""


class QualityReportUpsertRequest(BaseModel):
    """관리자 OCR 품질 리포트 alias 후보 등록 요청"""
    limit: int = Field(20, ge=1, le=200, description="분석할 샘플 수")
    confirm_write: bool = Field(False, description="DB write 명시 확인")


class QualityReportUpsertResponse(BaseModel):
    """관리자 OCR 품질 리포트 alias 후보 등록 응답"""
    success: bool
    upserted_count: int = 0
    candidate_count: int = 0
    skipped_count: int = 0
    message: str = ""
    generated_at: Optional[str] = None


class PendingConfirmationItem(BaseModel):
    """미확인 건 목록 항목"""
    request_id: str = Field(..., description="OCR 결과 request_id")
    raw_ocr_text: str = Field(..., description="OCR 원문")
    decision_status: str = Field(..., description="판정 상태")
    match_confidence: float = Field(0.0, description="매칭 신뢰도")
    best_drug_name: Optional[str] = Field(None, description="최고 후보 약품명")
    best_drug_item_seq: Optional[str] = Field(None, description="최고 후보 item_seq")
    candidates: List[dict] = Field(default_factory=list, description="후보 목록")
    created_at: Optional[str] = Field(None, description="생성 시점")
