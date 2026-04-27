from dependency_injector.wiring import Provide, inject
from fastapi import APIRouter, Depends, HTTPException, status

from app.core.container import Container
from app.core.middleware import inject_ocr
from app.ocr.services.ocr_service import OcrService
from app.ocr.services.medication_pipeline import MedicationPipeline
from app.ocr.schema.medication_schema import (
    MedicationOCRRequest,
    MedicationOCRResponse,
    MedicationInfo,
    PipelineStageInfo,
)


router = APIRouter(
    prefix="/ocr",
    tags=["ocr"],
)


@router.get(
    "",
    summary="OCR 서비스 테스트",
    description="OCR 서비스가 정상적으로 작동하는지 테스트합니다. (개발용)"
)
@inject_ocr
def test_ocr_service(
    service: OcrService = Depends(Provide[Container.ocr_service]),
):
    """OCR 서비스 테스트 엔드포인트"""
    return service.test()


@router.post(
    "/validate-medication",
    response_model=MedicationOCRResponse,
    summary="약 정보 OCR 검증 (파이프라인 v2)",
    description="OCR 텍스트 → 정규화 → MySQL 매칭 → VectorDB → 규칙 검증 → LLM 설명 생성"
)
@inject_ocr
async def validate_medication_ocr(
    request: MedicationOCRRequest,
    pipeline: MedicationPipeline = Depends(Provide[Container.medication_pipeline]),
):
    """
    OCR 약 식별 파이프라인 v2
    
    **처리 흐름:**
    1. OCR 텍스트 수신
    2. 정규화 (약품명 후보 추출 + 용량/제형 분리)
    3. MySQL 다중 매칭 (exact → prefix → ngram → fuzzy)
    4. VectorDB 2차 매칭 (score < 0.7인 경우)
    5. 규칙 검증 (업체/함량/제형/키워드 교차 검증)
    6. LLM 설명 생성 (어르신 맞춤)
    
    **예시 요청:**
    ```json
    {
      "ocr_text": "타이레놀정 500mg\\n1일 3회\\n식후 30분\\n1회 1정",
      "elderly_user_id": 123
    }
    ```
    """
    try:
        result = await pipeline.process(
            ocr_text=request.ocr_text,
            elderly_user_id=request.elderly_user_id,
        )

        # PipelineResult → MedicationOCRResponse 변환
        medications = []
        for candidate in result.identified_drugs:
            drug = candidate.drug_info
            medications.append(MedicationInfo(
                medication_name=drug.item_name,
                dosage=None,
                times=[],
                instructions=drug.use_method_qesitm[:200] if drug.use_method_qesitm else None,
                confidence=candidate.score,
                category=None,
                item_seq=drug.item_seq,
                entp_name=drug.entp_name,
                match_score=candidate.score,
                match_method=candidate.method,
                purpose=drug.efcy_qesitm[:200] if drug.efcy_qesitm else None,
                caution=drug.atpn_qesitm[:200] if drug.atpn_qesitm else None,
            ))

        pipeline_stages = [
            PipelineStageInfo(
                stage=s.stage,
                duration_ms=s.duration_ms,
                result_summary=s.result_summary,
            )
            for s in result.pipeline_stages
        ]

        response = MedicationOCRResponse(
            success=result.success,
            medications=medications,
            raw_ocr_text=result.raw_ocr_text,
            llm_analysis=result.llm_description,
            warnings=result.warnings,
            error_message=result.error_message,
            pipeline_stages=pipeline_stages,
            total_duration_ms=result.total_duration_ms,
        )

        if not response.success and response.error_message:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail=response.error_message
            )

        return response

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"OCR 파이프라인 오류: {str(e)}"
        )