from datetime import datetime

import pytest
from fastapi import HTTPException

from app.api.endpoints.ocr import get_vector_status, verify_internal_secret
from app.core.config import configs
from app.ocr.repository.drug_vector_repository import DrugVectorRepository


class StubCollection:
    def __init__(self, count=None, error=None):
        self._count = count
        self._error = error

    def count(self):
        if self._error:
            raise self._error
        return self._count


def make_repo(count=None, error=None):
    repo = object.__new__(DrugVectorRepository)
    repo.persist_directory = "./test_chroma_db"
    repo.collection_name = "drug_embeddings"
    repo._collection = StubCollection(count=count, error=error)
    return repo


def test_vector_status_ready_when_count_matches_expected():
    status = make_repo(count=35291).get_status(expected_count=35291)

    assert status["status"] == "READY"
    assert status["count"] == 35291
    assert status["expected_count"] == 35291
    assert status["is_degraded"] is False
    assert isinstance(status["checked_at"], datetime)


def test_vector_status_ready_when_expected_count_is_not_configured():
    status = make_repo(count=10).get_status(expected_count=0)

    assert status["status"] == "READY"
    assert status["expected_count"] is None
    assert status["is_degraded"] is False


def test_vector_status_empty_is_degraded_warning():
    status = make_repo(count=0).get_status(expected_count=35291)

    assert status["status"] == "EMPTY"
    assert status["count"] == 0
    assert status["is_degraded"] is True


def test_vector_status_count_mismatch_is_degraded_warning():
    status = make_repo(count=100).get_status(expected_count=35291)

    assert status["status"] == "COUNT_MISMATCH"
    assert status["count"] == 100
    assert status["is_degraded"] is True


def test_vector_status_error_is_degraded_warning():
    status = make_repo(error=RuntimeError("boom")).get_status(expected_count=35291)

    assert status["status"] == "ERROR"
    assert status["count"] is None
    assert status["is_degraded"] is True


@pytest.mark.asyncio
async def test_vector_status_endpoint_returns_repository_status():
    response = await get_vector_status(None, make_repo(count=35291))

    assert response.status == "READY"
    assert response.collection_name == "drug_embeddings"
    assert response.count == 35291


def test_vector_status_internal_secret_rejects_missing_secret():
    with pytest.raises(HTTPException) as exc:
        verify_internal_secret(None)

    assert exc.value.status_code == 401


def test_vector_status_internal_secret_rejects_wrong_secret():
    with pytest.raises(HTTPException) as exc:
        verify_internal_secret("wrong-secret")

    assert exc.value.status_code == 401


def test_vector_status_internal_secret_accepts_configured_secret():
    verify_internal_secret(configs.SILVERLINK_INTERNAL_SECRET)
