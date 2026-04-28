from typing import List
from dependency_injector.wiring import Provide, inject
from fastapi import APIRouter, Depends, HTTPException, status

from app.core.container import Container
from app.core.middleware import inject_ocr
from app.ocr.services.ocr_service import OcrService
from app.ocr.services.medication_pipeline import MedicationPipeline
from app.ocr.repository.ocr_result_repository import OcrResultRepository
from app.ocr.repository.alias_suggestion_repository import AliasSuggestionRepository
from app.ocr.schema.medication_schema import (
    MedicationOCRRequest,
    MedicationOCRResponse,
    MedicationInfo,
    PipelineStageInfo,
    ConfirmMedicationRequest,
    ConfirmMedicationResponse,
    PendingConfirmationItem,
)
from app.ocr.model.drug_model import OCRToken


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
            ocr_tokens=[
                OCRToken(value=token.value, confidence=token.confidence)
                for token in request.ocr_tokens
            ],
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
                evidence=candidate.evidence,
                validation_messages=candidate.validation_messages,
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
            decision_status=result.decision_status,
            match_confidence=result.match_confidence,
            requires_user_confirmation=result.requires_user_confirmation,
            decision_reasons=result.decision_reasons,
            request_id=result.request_id,
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


@router.post(
    "/confirm-medication",
    response_model=ConfirmMedicationResponse,
    summary="사용자 약품 후보 확인/거부",
    description="OCR 결과에서 사용자가 후보를 선택하거나 거부합니다.",
)
@inject_ocr
async def confirm_medication(
    request: ConfirmMedicationRequest,
    ocr_result_repo: OcrResultRepository = Depends(Provide[Container.ocr_result_repository]),
    alias_suggestion_repo: AliasSuggestionRepository = Depends(Provide[Container.alias_suggestion_repository]),
):
    try:
        # 1. OCR 결과 조회
        ocr_result = ocr_result_repo.get_result(request.request_id)
        if not ocr_result:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"OCR 결과를 찾을 수 없습니다: {request.request_id}",
            )

        if ocr_result.user_confirmed is not None:
            return ConfirmMedicationResponse(
                success=False,
                message="이미 확인된 요청입니다.",
            )

        # 2. 사용자 확인 결과 업데이트
        updated = ocr_result_repo.update_user_confirmation(
            request_id=request.request_id,
            selected_item_seq=request.selected_item_seq,
            confirmed=request.confirmed,
        )
        if not updated:
            return ConfirmMedicationResponse(
                success=False,
                message="확인 결과 업데이트 실패",
            )

        # 3. 확정 시 alias 제안 저장 (PENDING)
        alias_created = False
        if (
            request.confirmed
            and ocr_result.decision_status in ("NEED_USER_CONFIRMATION", "AMBIGUOUS")
            and ocr_result.raw_ocr_text
        ):
            from app.ocr.services.text_normalizer import TextNormalizer

            normalizer = TextNormalizer()
            normalized = normalizer.normalize(ocr_result.raw_ocr_text)
            if normalized:
                alias_text = normalized[0].name
                # best_drug_item_seq와 사용자 선택이 다르면 error_alias,
                # 같으면 alias (이름 변형)로 분류
                stype = "error_alias"
                if (
                    ocr_result.best_drug_item_seq
                    and ocr_result.best_drug_item_seq == request.selected_item_seq
                ):
                    stype = "alias"

                alias_created = alias_suggestion_repo.save_suggestion(
                    item_seq=request.selected_item_seq,
                    alias_name=alias_text,
                    alias_normalized=alias_text,
                    source_request_id=request.request_id,
                    source="user_feedback",
                    suggestion_type=stype,
                )

        return ConfirmMedicationResponse(
            success=True,
            message="확인 처리 완료" if request.confirmed else "거부 처리 완료",
            alias_suggestion_created=alias_created,
        )

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"확인 처리 오류: {str(e)}",
        )


@router.get(
    "/pending-confirmations/{elderly_user_id}",
    response_model=List[PendingConfirmationItem],
    summary="미확인 OCR 결과 목록 조회",
    description="사용자 확인이 필요한 OCR 결과 목록을 조회합니다.",
)
@inject_ocr
async def get_pending_confirmations(
    elderly_user_id: int,
    ocr_result_repo: OcrResultRepository = Depends(Provide[Container.ocr_result_repository]),
):
    try:
        records = ocr_result_repo.get_pending_confirmations(elderly_user_id)
        return [
            PendingConfirmationItem(
                request_id=r.request_id,
                raw_ocr_text=r.raw_ocr_text,
                decision_status=r.decision_status,
                match_confidence=r.match_confidence,
                best_drug_name=r.best_drug_name,
                best_drug_item_seq=r.best_drug_item_seq,
                candidates=r.candidates,
                created_at=r.created_at.isoformat() if r.created_at else None,
            )
            for r in records
        ]
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"미확인 목록 조회 오류: {str(e)}",
        )


# ============================================================
# 관리자 Alias 승인/거부 API (Phase 9)
# ============================================================


@router.get(
    "/admin/alias-suggestions",
    summary="[관리자] PENDING alias 제안 목록 (페이징)",
    description="관리자가 검토할 alias 제안 목록을 페이징으로 조회합니다.",
)
@inject_ocr
async def get_alias_suggestions(
    page: int = 1,
    size: int = 20,
    review_status: str = "PENDING",
    alias_suggestion_repo: AliasSuggestionRepository = Depends(Provide[Container.alias_suggestion_repository]),
):
    try:
        result = alias_suggestion_repo.get_pending_suggestions_paged(
            page=page,
            size=size,
            review_status=review_status if review_status != "ALL" else None,
        )
        return result
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"alias 제안 조회 오류: {str(e)}",
        )


@router.put(
    "/admin/alias-suggestions/{suggestion_id}/approve",
    summary="[관리자] alias 제안 승인",
    description="PENDING 상태의 alias 제안을 승인하여 실제 alias/error_alias 테이블에 등록합니다.",
)
@inject_ocr
async def approve_alias_suggestion(
    suggestion_id: int,
    reviewed_by: str = "admin",
    alias_suggestion_repo: AliasSuggestionRepository = Depends(Provide[Container.alias_suggestion_repository]),
):
    try:
        result = alias_suggestion_repo.approve_suggestion(
            suggestion_id=suggestion_id,
            reviewed_by=reviewed_by,
        )
        if not result["success"]:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=result["message"],
            )
        return result
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"alias 제안 승인 오류: {str(e)}",
        )


@router.put(
    "/admin/alias-suggestions/{suggestion_id}/reject",
    summary="[관리자] alias 제안 거부",
    description="PENDING 상태의 alias 제안을 거부합니다.",
)
@inject_ocr
async def reject_alias_suggestion(
    suggestion_id: int,
    reviewed_by: str = "admin",
    alias_suggestion_repo: AliasSuggestionRepository = Depends(Provide[Container.alias_suggestion_repository]),
):
    try:
        result = alias_suggestion_repo.reject_suggestion(
            suggestion_id=suggestion_id,
            reviewed_by=reviewed_by,
        )
        if not result["success"]:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=result["message"],
            )
        return result
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"alias 제안 거부 오류: {str(e)}",
        )


@router.post(
    "/admin/reload-dictionary",
    summary="[관리자] LocalDrugIndex 리로드",
    description="alias 승인 후 인메모리 약품 인덱스를 갱신합니다.",
)
@inject_ocr
async def reload_dictionary(
    pipeline: MedicationPipeline = Depends(Provide[Container.medication_pipeline]),
):
    try:
        if hasattr(pipeline, 'drug_index') and pipeline.drug_index:
            pipeline.drug_index.reload()
            return {"success": True, "message": "LocalDrugIndex 리로드 완료"}
        return {"success": True, "message": "DrugIndex 없음 (리로드 불필요)"}
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"리로드 오류: {str(e)}",
        )
