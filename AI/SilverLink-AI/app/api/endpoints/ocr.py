from typing import List

from dependency_injector.wiring import Provide
from fastapi import APIRouter, Depends, HTTPException, status

from app.core.container import Container
from app.core.middleware import inject_ocr
from app.ocr.model.drug_model import OCRToken
from app.ocr.repository.alias_suggestion_repository import AliasSuggestionRepository
from app.ocr.repository.ocr_result_repository import OcrResultRepository
from app.ocr.schema.medication_schema import (
    ConfirmMedicationRequest,
    ConfirmMedicationResponse,
    MedicationInfo,
    MedicationOCRRequest,
    MedicationOCRResponse,
    PendingConfirmationItem,
    PipelineStageInfo,
)
from app.ocr.services.medication_pipeline import MedicationPipeline
from app.ocr.services.ocr_service import OcrService


router = APIRouter(prefix="/ocr", tags=["ocr"])


@router.get("", summary="OCR service health check")
@inject_ocr
def test_ocr_service(
    service: OcrService = Depends(Provide[Container.ocr_service]),
):
    return service.test()


@router.post(
    "/validate-medication",
    response_model=MedicationOCRResponse,
    summary="Validate medication OCR text",
)
@inject_ocr
async def validate_medication_ocr(
    request: MedicationOCRRequest,
    pipeline: MedicationPipeline = Depends(Provide[Container.medication_pipeline]),
):
    try:
        result = await pipeline.process(
            ocr_text=request.ocr_text,
            elderly_user_id=request.elderly_user_id,
            ocr_tokens=[
                OCRToken(value=token.value, confidence=token.confidence)
                for token in request.ocr_tokens
            ],
        )

        medications = []
        for candidate in result.identified_drugs:
            drug = candidate.drug_info
            medications.append(
                MedicationInfo(
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
                )
            )

        pipeline_stages = [
            PipelineStageInfo(
                stage=stage.stage,
                duration_ms=stage.duration_ms,
                result_summary=stage.result_summary,
            )
            for stage in result.pipeline_stages
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
                detail=response.error_message,
            )

        return response
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"OCR pipeline error: {exc}",
        )


@router.post(
    "/confirm-medication",
    response_model=ConfirmMedicationResponse,
    summary="Confirm or reject a medication OCR candidate",
)
@inject_ocr
async def confirm_medication(
    request: ConfirmMedicationRequest,
    ocr_result_repo: OcrResultRepository = Depends(Provide[Container.ocr_result_repository]),
    alias_suggestion_repo: AliasSuggestionRepository = Depends(Provide[Container.alias_suggestion_repository]),
):
    try:
        ocr_result = ocr_result_repo.get_result(request.request_id)
        if not ocr_result:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"OCR result not found: {request.request_id}",
            )

        if ocr_result.user_confirmed is not None:
            return ConfirmMedicationResponse(
                success=False,
                message="This request has already been confirmed.",
            )

        updated = ocr_result_repo.update_user_confirmation(
            request_id=request.request_id,
            selected_item_seq=request.selected_item_seq,
            confirmed=request.confirmed,
        )
        if not updated:
            return ConfirmMedicationResponse(
                success=False,
                message="Failed to update confirmation result.",
            )

        alias_created = False
        if (
            request.confirmed
            and ocr_result.decision_status in ("NEED_USER_CONFIRMATION", "AMBIGUOUS")
            and ocr_result.raw_ocr_text
        ):
            from app.ocr.services.text_normalizer import TextNormalizer

            normalized = TextNormalizer().normalize(ocr_result.raw_ocr_text)
            if normalized:
                alias_text = normalized[0].name
                suggestion_type = "error_alias"
                if (
                    ocr_result.best_drug_item_seq
                    and ocr_result.best_drug_item_seq == request.selected_item_seq
                ):
                    suggestion_type = "alias"

                alias_created = alias_suggestion_repo.save_suggestion(
                    item_seq=request.selected_item_seq,
                    alias_name=alias_text,
                    alias_normalized=alias_text,
                    source_request_id=request.request_id,
                    source="user_feedback",
                    suggestion_type=suggestion_type,
                )

        return ConfirmMedicationResponse(
            success=True,
            message="Confirmation processed." if request.confirmed else "Rejection processed.",
            alias_suggestion_created=alias_created,
        )
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Confirmation error: {exc}",
        )


@router.get(
    "/pending-confirmations/{elderly_user_id}",
    response_model=List[PendingConfirmationItem],
    summary="List pending OCR confirmations",
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
                request_id=record.request_id,
                raw_ocr_text=record.raw_ocr_text,
                decision_status=record.decision_status,
                match_confidence=record.match_confidence,
                best_drug_name=record.best_drug_name,
                best_drug_item_seq=record.best_drug_item_seq,
                candidates=record.candidates,
                created_at=record.created_at.isoformat() if record.created_at else None,
            )
            for record in records
        ]
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Pending confirmation lookup error: {exc}",
        )


@router.get(
    "/admin/alias-suggestions",
    summary="List alias suggestions for admin review",
)
@inject_ocr
async def get_alias_suggestions(
    page: int = 1,
    size: int = 20,
    review_status: str = "PENDING",
    alias_suggestion_repo: AliasSuggestionRepository = Depends(Provide[Container.alias_suggestion_repository]),
):
    try:
        return alias_suggestion_repo.get_pending_suggestions_paged(
            page=page,
            size=size,
            review_status=review_status if review_status != "ALL" else None,
        )
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Alias suggestion lookup error: {exc}",
        )


@router.put(
    "/admin/alias-suggestions/{suggestion_id}/approve",
    summary="Approve an alias suggestion",
    description="Approve a pending alias suggestion and reload the in-memory dictionary.",
)
@inject_ocr
async def approve_alias_suggestion(
    suggestion_id: int,
    reviewed_by: str = "admin",
    alias_suggestion_repo: AliasSuggestionRepository = Depends(Provide[Container.alias_suggestion_repository]),
    pipeline: MedicationPipeline = Depends(Provide[Container.medication_pipeline]),
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

        # The approval transaction is committed inside the repository call.
        # Reload is best-effort: if it fails, DB changes remain valid and the
        # existing in-memory dictionary is kept until a retry or server restart.
        reload_ok = pipeline.reload_dictionary()
        if reload_ok:
            result["reload_success"] = True
            result["message"] = "Alias suggestion approved and dictionary reloaded."
        else:
            result["reload_success"] = False
            result["reload_warning"] = (
                "DB was updated, but dictionary reload failed. Restart AI server or retry reload."
            )

        result["suggestion_id"] = suggestion_id
        result["review_status"] = "APPROVED"
        return result
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Alias approval error: {exc}",
        )


@router.put(
    "/admin/alias-suggestions/{suggestion_id}/reject",
    summary="Reject an alias suggestion",
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

        result["suggestion_id"] = suggestion_id
        result["review_status"] = "REJECTED"
        return result
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Alias rejection error: {exc}",
        )


@router.post(
    "/admin/reload-dictionary",
    summary="Reload the local drug dictionary",
)
@inject_ocr
async def reload_dictionary(
    pipeline: MedicationPipeline = Depends(Provide[Container.medication_pipeline]),
):
    try:
        # TODO: Protect this endpoint as admin-only or internal-only in production.
        # TODO: In a multi-worker Uvicorn deployment, this refreshes only the
        # worker that handles the request. Use rolling restart, version polling,
        # Redis pub/sub, or a similar synchronization strategy for production.
        ok = pipeline.reload_dictionary()
        if ok:
            return {"success": True, "message": "LocalDrugIndex reload completed"}
        return {"success": False, "message": "LocalDrugIndex reload failed. Existing index was kept."}
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Dictionary reload error: {exc}",
        )
