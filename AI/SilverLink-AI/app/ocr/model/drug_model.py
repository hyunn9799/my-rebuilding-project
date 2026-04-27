"""
OCR 약 식별 파이프라인 Pydantic 모델
"""
from typing import Optional, List
from pydantic import BaseModel, Field


class DrugInfo(BaseModel):
    """e약은요 API 기반 약품 마스터 데이터"""
    id: Optional[int] = None
    item_seq: str = Field(..., description="품목기준코드")
    item_name: str = Field(..., description="약품명")
    item_name_normalized: Optional[str] = Field(None, description="정규화된 약품명")
    entp_name: Optional[str] = Field(None, description="업체명")
    efcy_qesitm: Optional[str] = Field(None, description="효능효과")
    use_method_qesitm: Optional[str] = Field(None, description="사용법")
    atpn_qesitm: Optional[str] = Field(None, description="주의사항")
    intrc_qesitm: Optional[str] = Field(None, description="상호작용")
    se_qesitm: Optional[str] = Field(None, description="부작용")
    deposit_method_qesitm: Optional[str] = Field(None, description="보관법")
    item_image: Optional[str] = Field(None, description="이미지URL")


class MatchCandidate(BaseModel):
    """매칭 후보 결과"""
    drug_info: DrugInfo
    score: float = Field(..., ge=0.0, le=1.0, description="매칭 점수")
    method: str = Field(..., description="매칭 방법 (exact/prefix/ngram/fuzzy/vector)")


class MatchResult(BaseModel):
    """매칭 단계 결과"""
    candidates: List[MatchCandidate] = Field(default_factory=list)
    best_score: float = Field(0.0, description="최고 점수")
    method: str = Field("none", description="최고 점수 매칭 방법")


class NormalizedDrug(BaseModel):
    """정규화된 약품명 후보"""
    name: str = Field(..., description="정규화된 약품명")
    dosage: Optional[str] = Field(None, description="분리된 용량 (500mg 등)")
    form_type: Optional[str] = Field(None, description="제형 (정, 캡슐, 시럽 등)")
    original: str = Field(..., description="원본 텍스트")


class PipelineStage(BaseModel):
    """파이프라인 단계별 결과"""
    stage: str = Field(..., description="단계명")
    duration_ms: float = Field(..., description="소요시간(ms)")
    result_summary: str = Field("", description="결과 요약")


class PipelineResult(BaseModel):
    """파이프라인 전체 결과"""
    success: bool = Field(False)
    identified_drugs: List[MatchCandidate] = Field(default_factory=list)
    raw_ocr_text: str = Field("")
    normalized_names: List[NormalizedDrug] = Field(default_factory=list)
    llm_description: str = Field("")
    warnings: List[str] = Field(default_factory=list)
    pipeline_stages: List[PipelineStage] = Field(default_factory=list)
    total_duration_ms: float = Field(0.0)
    error_message: Optional[str] = None
