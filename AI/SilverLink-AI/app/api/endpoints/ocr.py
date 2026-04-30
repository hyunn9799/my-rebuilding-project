from collections import Counter
import json
from pathlib import Path
from typing import List

from dependency_injector.wiring import Provide
from fastapi import APIRouter, Depends, Header, HTTPException, status

from app.core.container import Container
from app.core.config import configs
from app.core.middleware import inject_ocr
from app.ocr.model.drug_model import OCRToken
from app.ocr.repository.alias_suggestion_repository import AliasSuggestionRepository
from app.ocr.repository.drug_vector_repository import DrugVectorRepository
from app.ocr.repository.ocr_result_repository import OcrResultRepository
from app.ocr.schema.medication_schema import (
    ConfirmMedicationRequest,
    ConfirmMedicationResponse,
    MedicationInfo,
    MedicationOCRRequest,
    MedicationOCRResponse,
    OcrResultOwnerResponse,
    PendingConfirmationItem,
    PipelineStageInfo,
    QualityReportRunRequest,
    QualityReportRunResponse,
    QualityReportUpsertRequest,
    QualityReportUpsertResponse,
    VectorStatusResponse,
)
from app.ocr.services.medication_pipeline import MedicationPipeline
from app.ocr.services.ocr_service import OcrService
from scripts import analyze_ocr_quality


router = APIRouter(prefix="/ocr", tags=["ocr"])


def verify_internal_secret(
    secret: str | None = Header(None, alias=configs.CHATBOT_SECRET_HEADER),
) -> None:
    if not secret or secret != configs.SILVERLINK_INTERNAL_SECRET:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid internal secret",
        )


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
    "/internal/results/{request_id}/owner",
    response_model=OcrResultOwnerResponse,
    summary="Lookup OCR result owner by requestId",
)
@inject_ocr
async def get_ocr_result_owner(
    request_id: str,
    _: None = Depends(verify_internal_secret),
    ocr_result_repo: OcrResultRepository = Depends(Provide[Container.ocr_result_repository]),
):
    owner = ocr_result_repo.get_owner_by_request_id(request_id)
    if not owner:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"OCR result not found: {request_id}",
        )
    if owner.get("user_confirmed") is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="This request has already been confirmed.",
        )
    if owner.get("elderly_user_id") is None:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="OCR result has no elderly_user_id.",
        )
    return OcrResultOwnerResponse(**owner)


@router.get(
    "/internal/vector/status",
    response_model=VectorStatusResponse,
    summary="Lookup ChromaDB vector fallback status",
)
@inject_ocr
async def get_vector_status(
    _: None = Depends(verify_internal_secret),
    vector_repo: DrugVectorRepository = Depends(Provide[Container.drug_vector_repository]),
):
    status_payload = vector_repo.get_status(configs.DRUG_VECTOR_EXPECTED_COUNT)
    return VectorStatusResponse(**status_payload)


def _quality_action_counts(actions: list[dict]) -> Counter:
    return Counter(str(action.get("action_type") or "") for action in actions if action.get("action_type"))


def _quality_report_response(metrics: dict, include_candidates: bool) -> QualityReportRunResponse:
    actions = metrics.get("recommended_actions", [])
    action_counts = _quality_action_counts(actions)
    alias_candidates = analyze_ocr_quality.alias_candidate_payload(actions)
    return QualityReportRunResponse(
        success=True,
        generated_at=metrics.get("generated_at", ""),
        decision_counts=metrics.get("decision_counts", []),
        suggestion_counts=metrics.get("suggestion_counts", []),
        match_method_counts=metrics.get("match_method_counts", []),
        recommended_action_counts=dict(action_counts),
        alias_candidate_count=action_counts.get("alias_candidate", 0) + action_counts.get("error_alias_candidate", 0),
        manual_review_count=action_counts.get("manual_review", 0),
        normalization_candidate_count=action_counts.get("normalization_candidate", 0),
        report_markdown=analyze_ocr_quality.render_markdown(metrics),
        alias_candidates=alias_candidates if include_candidates else [],
        message="OCR quality report generated.",
    )


@router.post(
    "/admin/quality-report/run",
    response_model=QualityReportRunResponse,
    summary="Run OCR quality report for admin review",
)
@inject_ocr
async def run_quality_report(
    request: QualityReportRunRequest,
    _: None = Depends(verify_internal_secret),
):
    try:
        metrics = analyze_ocr_quality.collect_quality_metrics(limit=request.limit)
        response = _quality_report_response(metrics, request.include_candidates)
        if request.persist_files:
            Path("docs").mkdir(parents=True, exist_ok=True)
            Path("docs/ocr_quality_report.md").write_text(response.report_markdown, encoding="utf-8")
            Path("docs/ocr_alias_candidates.json").write_text(
                json.dumps(response.alias_candidates, ensure_ascii=False, indent=2),
                encoding="utf-8",
            )
        return response
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"OCR quality report error: {exc}",
        )


@router.post(
    "/admin/quality-report/upsert-alias-candidates",
    response_model=QualityReportUpsertResponse,
    summary="Upsert OCR quality report alias candidates",
)
@inject_ocr
async def upsert_quality_report_alias_candidates(
    request: QualityReportUpsertRequest,
    _: None = Depends(verify_internal_secret),
):
    if not request.confirm_write:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="confirm_write must be true to upsert alias candidates.",
        )

    try:
        metrics = analyze_ocr_quality.collect_quality_metrics(limit=request.limit)
        actions = metrics.get("recommended_actions", [])
        candidates = analyze_ocr_quality.alias_candidate_payload(actions)
        upserted_count = analyze_ocr_quality.upsert_alias_candidates(actions)
        return QualityReportUpsertResponse(
            success=True,
            upserted_count=upserted_count,
            candidate_count=len(candidates),
            skipped_count=max(0, len(actions) - len(candidates)),
            message=f"Upserted {upserted_count} alias candidate(s).",
            generated_at=metrics.get("generated_at"),
        )
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"OCR quality alias candidate upsert error: {exc}",
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
    _: None = Depends(verify_internal_secret),
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
    _: None = Depends(verify_internal_secret),
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
    _: None = Depends(verify_internal_secret),
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
    _: None = Depends(verify_internal_secret),
    pipeline: MedicationPipeline = Depends(Provide[Container.medication_pipeline]),
):
    try:
        # TODO: Protect this endpoint as admin-only or internal-only in production.
        # TODO: In a multi-worker Uvicorn deployment, this refreshes only the
        # worker that handles the request. Use rolling restart, version polling,
        # Redis pub/sub, or a similar synchronization strategy for production.
        ok = pipeline.reload_dictionary()
        reload_stats = pipeline.reload_dictionary_stats()
        if ok:
            return {
                "success": True,
                "message": "LocalDrugIndex reload completed",
                **reload_stats,
            }
        return {
            "success": False,
            "message": "LocalDrugIndex reload failed. Existing index was kept.",
            **reload_stats,
        }
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Dictionary reload error: {exc}",
        )
