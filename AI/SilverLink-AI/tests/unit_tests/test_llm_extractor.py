"""Phase 6: LLM Extractor 단위 테스트."""
import asyncio
import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.ocr.model.drug_model import NormalizedDrug
from app.ocr.services.llm_extractor import LLMExtractor


# ──────────────────────────────────────────────
# Helpers
# ──────────────────────────────────────────────

def _make_fake_llm():
    """AsyncOpenAI mock을 가진 fake LLM."""
    llm = MagicMock()
    llm.model_version = "gpt-4o-mini"
    llm.aclient = MagicMock()
    return llm


def _make_completion_response(medications_json: list) -> MagicMock:
    """LLM completion 응답 mock."""
    response_text = json.dumps({"medications": medications_json}, ensure_ascii=False)
    mock_choice = MagicMock()
    mock_choice.message.content = response_text
    mock_completion = MagicMock()
    mock_completion.choices = [mock_choice]
    return mock_completion


# ──────────────────────────────────────────────
# Tests
# ──────────────────────────────────────────────

def test_llm_extractor_parses_valid_response():
    """LLM이 정상 JSON을 반환하면 NormalizedDrug 리스트로 파싱."""
    llm = _make_fake_llm()
    extractor = LLMExtractor(llm=llm)

    medications = [
        {"name": "타이레놀정", "dosage": "500mg", "form_type": "정", "original_fragment": "타이레놀정 500mg"},
        {"name": "아목시실린캡슐", "dosage": "250mg", "form_type": "캡슐", "original_fragment": "아목시실린캡슐 250mg"},
    ]
    mock_completion = _make_completion_response(medications)
    llm.aclient.chat.completions.create = AsyncMock(return_value=mock_completion)

    ocr_text = "타이레놀정 500mg\n아목시실린캡슐 250mg"
    result = asyncio.get_event_loop().run_until_complete(extractor.extract(ocr_text))

    assert len(result) == 2
    assert result[0].name == "타이레놀정"
    assert result[0].dosage == "500mg"
    assert result[0].source == "llm_hint"
    assert result[1].name == "아목시실린캡슐"
    assert result[1].source == "llm_hint"


def test_llm_extractor_returns_empty_on_disabled():
    """enabled=False이면 빈 리스트 반환."""
    llm = _make_fake_llm()
    extractor = LLMExtractor(llm=llm, enabled=False)

    result = asyncio.get_event_loop().run_until_complete(extractor.extract("타이레놀정 500mg"))
    assert result == []


def test_llm_extractor_returns_empty_on_failure():
    """LLM 호출 실패 시 빈 리스트 반환 (non-blocking)."""
    llm = _make_fake_llm()
    llm.aclient.chat.completions.create = AsyncMock(side_effect=Exception("API error"))
    extractor = LLMExtractor(llm=llm)

    result = asyncio.get_event_loop().run_until_complete(extractor.extract("타이레놀정 500mg"))
    assert result == []


def test_llm_extractor_filters_hallucination():
    """OCR 원문에 없는 약품명은 hallucination으로 필터링."""
    llm = _make_fake_llm()
    extractor = LLMExtractor(llm=llm)

    medications = [
        {"name": "타이레놀정", "dosage": "500mg", "form_type": "정", "original_fragment": "타이레놀정 500mg"},
        {"name": "존재하지않는약품", "dosage": "100mg", "form_type": "정", "original_fragment": "존재하지않는약품"},
    ]
    mock_completion = _make_completion_response(medications)
    llm.aclient.chat.completions.create = AsyncMock(return_value=mock_completion)

    ocr_text = "타이레놀정 500mg\n아목시실린캡슐 250mg"
    result = asyncio.get_event_loop().run_until_complete(extractor.extract(ocr_text))

    assert len(result) == 1
    assert result[0].name == "타이레놀정"


def test_llm_extractor_deduplicates():
    """동일 약품명 중복 제거."""
    llm = _make_fake_llm()
    extractor = LLMExtractor(llm=llm)

    medications = [
        {"name": "타이레놀정", "dosage": "500mg", "form_type": "정", "original_fragment": "타이레놀정"},
        {"name": "타이레놀정", "dosage": "500mg", "form_type": "정", "original_fragment": "타이레놀정"},
    ]
    mock_completion = _make_completion_response(medications)
    llm.aclient.chat.completions.create = AsyncMock(return_value=mock_completion)

    result = asyncio.get_event_loop().run_until_complete(extractor.extract("타이레놀정 500mg"))
    assert len(result) == 1


def test_llm_extractor_handles_empty_medications():
    """약품을 못 찾으면 빈 배열 반환."""
    llm = _make_fake_llm()
    extractor = LLMExtractor(llm=llm)

    mock_completion = _make_completion_response([])
    llm.aclient.chat.completions.create = AsyncMock(return_value=mock_completion)

    result = asyncio.get_event_loop().run_until_complete(extractor.extract("환자명 홍길동"))
    assert result == []


def test_verify_in_source_hangul():
    """한글 검증: OCR 원문에 한글 2글자 이상 포함 확인."""
    assert LLMExtractor._verify_in_source("타이레놀정", "타이레놀정 500mg") is True
    assert LLMExtractor._verify_in_source("타이레놀정", "아목시실린 250mg") is False


def test_verify_in_source_latin():
    """영문 검증: OCR 원문에 영문 3글자 이상 포함 확인."""
    assert LLMExtractor._verify_in_source("Amoxicillin", "Amoxicillin 250mg") is True
    assert LLMExtractor._verify_in_source("Amoxicillin", "타이레놀 500mg") is False


def test_llm_extractor_handles_json_in_code_block():
    """```json ... ``` 블록 응답도 정상 파싱."""
    llm = _make_fake_llm()
    extractor = LLMExtractor(llm=llm)

    response_text = '```json\n{"medications": [{"name": "타이레놀정", "dosage": "500mg", "form_type": "정", "original_fragment": "타이레놀정"}]}\n```'
    mock_choice = MagicMock()
    mock_choice.message.content = response_text
    mock_completion = MagicMock()
    mock_completion.choices = [mock_choice]
    llm.aclient.chat.completions.create = AsyncMock(return_value=mock_completion)

    result = asyncio.get_event_loop().run_until_complete(extractor.extract("타이레놀정 500mg"))
    assert len(result) == 1
    assert result[0].name == "타이레놀정"
