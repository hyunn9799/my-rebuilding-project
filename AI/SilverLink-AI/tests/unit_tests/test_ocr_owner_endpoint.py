import pytest
from fastapi import HTTPException

from app.core.config import configs
from app.api.endpoints.ocr import get_ocr_result_owner, verify_internal_secret


class StubOcrResultRepository:
    def __init__(self, owner):
        self.owner = owner
        self.request_ids = []

    def get_owner_by_request_id(self, request_id):
        self.request_ids.append(request_id)
        return self.owner


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
