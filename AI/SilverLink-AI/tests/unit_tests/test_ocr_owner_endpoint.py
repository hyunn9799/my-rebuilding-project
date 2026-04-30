import pytest
from fastapi import HTTPException

from app.core.config import configs
from app.api.endpoints import ocr as ocr_endpoint
from app.api.endpoints.ocr import (
    get_ocr_result_owner,
    reload_dictionary,
    run_quality_report,
    upsert_quality_report_alias_candidates,
    verify_internal_secret,
)
from app.ocr.schema.medication_schema import QualityReportRunRequest, QualityReportUpsertRequest


class StubOcrResultRepository:
    def __init__(self, owner):
        self.owner = owner
        self.request_ids = []

    def get_owner_by_request_id(self, request_id):
        self.request_ids.append(request_id)
        return self.owner


class StubPipeline:
    def __init__(self, ok=True):
        self.ok = ok

    def reload_dictionary(self):
        return self.ok

    def reload_dictionary_stats(self):
        return {
            "elapsed_ms": 12.3,
            "drug_count": 2,
            "alias_count": 1,
            "error_alias_count": 0,
            "existing_index_kept": not self.ok,
        }


@pytest.mark.asyncio
async def test_get_ocr_result_owner_success():
    repo = StubOcrResultRepository(
        {
            "request_id": "req-1",
            "elderly_user_id": 10,
            "decision_status": "NEED_USER_CONFIRMATION",
            "user_confirmed": None,
        }
    )

    response = await get_ocr_result_owner("req-1", None, repo)

    assert response.request_id == "req-1"
    assert response.elderly_user_id == 10
    assert response.decision_status == "NEED_USER_CONFIRMATION"
    assert response.user_confirmed is None
    assert repo.request_ids == ["req-1"]


@pytest.mark.asyncio
async def test_get_ocr_result_owner_not_found():
    repo = StubOcrResultRepository(None)

    with pytest.raises(HTTPException) as exc:
        await get_ocr_result_owner("missing", None, repo)

    assert exc.value.status_code == 404


@pytest.mark.asyncio
async def test_get_ocr_result_owner_conflict_when_already_confirmed():
    repo = StubOcrResultRepository(
        {
            "request_id": "req-1",
            "elderly_user_id": 10,
            "decision_status": "NEED_USER_CONFIRMATION",
            "user_confirmed": True,
        }
    )

    with pytest.raises(HTTPException) as exc:
        await get_ocr_result_owner("req-1", None, repo)

    assert exc.value.status_code == 409


@pytest.mark.asyncio
async def test_get_ocr_result_owner_rejects_missing_elderly_user_id():
    repo = StubOcrResultRepository(
        {
            "request_id": "req-1",
            "elderly_user_id": None,
            "decision_status": "NEED_USER_CONFIRMATION",
            "user_confirmed": None,
        }
    )

    with pytest.raises(HTTPException) as exc:
        await get_ocr_result_owner("req-1", None, repo)

    assert exc.value.status_code == 422


def test_verify_internal_secret_rejects_missing_secret():
    with pytest.raises(HTTPException) as exc:
        verify_internal_secret(None)

    assert exc.value.status_code == 401


def test_verify_internal_secret_rejects_wrong_secret():
    with pytest.raises(HTTPException) as exc:
        verify_internal_secret("wrong-secret")

    assert exc.value.status_code == 401


def test_verify_internal_secret_accepts_configured_secret():
    verify_internal_secret(configs.SILVERLINK_INTERNAL_SECRET)


@pytest.mark.asyncio
async def test_reload_dictionary_returns_operational_metrics():
    response = await reload_dictionary(None, StubPipeline(ok=True))

    assert response["success"] is True
    assert response["elapsed_ms"] == 12.3
    assert response["drug_count"] == 2
    assert response["alias_count"] == 1


@pytest.mark.asyncio
async def test_reload_dictionary_failure_reports_existing_index_kept():
    response = await reload_dictionary(None, StubPipeline(ok=False))

    assert response["success"] is False
    assert response["existing_index_kept"] is True
    assert response["drug_count"] == 2


@pytest.mark.asyncio
async def test_run_quality_report_returns_summary(monkeypatch):
    metrics = {
        "generated_at": "2026-04-30T10:00:00",
        "decision_counts": [{"decision_status": "MATCHED", "total": 3}],
        "suggestion_counts": [],
        "match_method_counts": [("prefix", 2)],
        "top_suggestions": [],
        "repeated_raw_texts": [],
        "recommended_actions": [
            {
                "action_type": "alias_candidate",
                "item_seq": "A001",
                "item_name": "테스트정500mg",
                "alias_name": "테스트정",
                "alias_normalized": "테스트정",
                "frequency": 1,
            },
            {"action_type": "manual_review", "raw_text": "성분명"},
            {"action_type": "normalization_candidate", "raw_text": "타이레놀정 5 O O mg"},
        ],
    }
    monkeypatch.setattr(ocr_endpoint.analyze_ocr_quality, "collect_quality_metrics", lambda limit: metrics)

    response = await run_quality_report(QualityReportRunRequest(limit=5), None)

    assert response.success is True
    assert response.generated_at == "2026-04-30T10:00:00"
    assert response.alias_candidate_count == 1
    assert response.manual_review_count == 1
    assert response.normalization_candidate_count == 1
    assert response.alias_candidates[0]["item_seq"] == "A001"
    assert response.alias_candidates[0]["priority_score"] == 28


@pytest.mark.asyncio
async def test_run_quality_report_can_omit_candidates(monkeypatch):
    metrics = {
        "generated_at": "2026-04-30T10:00:00",
        "decision_counts": [],
        "suggestion_counts": [],
        "match_method_counts": [],
        "top_suggestions": [],
        "repeated_raw_texts": [],
        "recommended_actions": [
            {
                "action_type": "alias_candidate",
                "item_seq": "A001",
                "alias_name": "테스트정",
                "alias_normalized": "테스트정",
            }
        ],
    }
    monkeypatch.setattr(ocr_endpoint.analyze_ocr_quality, "collect_quality_metrics", lambda limit: metrics)

    response = await run_quality_report(QualityReportRunRequest(include_candidates=False), None)

    assert response.alias_candidate_count == 1
    assert response.alias_candidates == []


@pytest.mark.asyncio
async def test_upsert_quality_report_alias_candidates_requires_confirm_write():
    with pytest.raises(HTTPException) as exc:
        await upsert_quality_report_alias_candidates(QualityReportUpsertRequest(confirm_write=False), None)

    assert exc.value.status_code == 400


@pytest.mark.asyncio
async def test_upsert_quality_report_alias_candidates_returns_counts(monkeypatch):
    metrics = {
        "generated_at": "2026-04-30T10:00:00",
        "recommended_actions": [
            {
                "action_type": "alias_candidate",
                "item_seq": "A001",
                "alias_name": "테스트정",
                "alias_normalized": "테스트정",
            },
            {"action_type": "manual_review", "raw_text": "성분명"},
        ],
    }
    monkeypatch.setattr(ocr_endpoint.analyze_ocr_quality, "collect_quality_metrics", lambda limit: metrics)
    monkeypatch.setattr(ocr_endpoint.analyze_ocr_quality, "upsert_alias_candidates", lambda actions: 1)

    response = await upsert_quality_report_alias_candidates(
        QualityReportUpsertRequest(limit=5, confirm_write=True),
        None,
    )

    assert response.success is True
    assert response.upserted_count == 1
    assert response.candidate_count == 1
    assert response.skipped_count == 1
