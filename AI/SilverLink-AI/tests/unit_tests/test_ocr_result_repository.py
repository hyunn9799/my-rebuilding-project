import json

from app.ocr.repository.ocr_result_repository import OcrResultRepository


def test_row_to_record_restores_candidates_json_policy():
    row = {
        "id": 1,
        "request_id": "req-1",
        "elderly_user_id": 10,
        "raw_ocr_text": "타이레놀정 500mg",
        "normalized_names": json.dumps(
            [{"name": "타이레놀정", "dosage": "500mg"}],
            ensure_ascii=False,
        ),
        "candidates": json.dumps(
            [
                {
                    "drug_info": {
                        "item_seq": "ITEM-1",
                        "item_name": "타이레놀정500밀리그람",
                    },
                    "score": 0.93,
                    "method": "exact",
                    "evidence": {
                        "item_seq": "ITEM-1",
                        "match_method": "local_exact",
                    },
                    "validation_messages": ["strength match"],
                }
            ],
            ensure_ascii=False,
        ),
        "pipeline_stages": json.dumps(
            [{"stage": "match", "duration_ms": 12.5}],
            ensure_ascii=False,
        ),
        "decision_status": "NEED_USER_CONFIRMATION",
        "match_confidence": 0.83,
        "decision_reasons": json.dumps(["vector-only candidate requires confirmation"], ensure_ascii=False),
        "best_drug_item_seq": "ITEM-1",
        "best_drug_name": "타이레놀정500밀리그람",
        "user_confirmed": None,
        "user_selected_seq": None,
        "user_confirmed_at": None,
        "llm_description": "해열진통제 후보입니다.",
        "warnings": json.dumps(["사용자 확인 필요"], ensure_ascii=False),
        "total_duration_ms": 123.4,
        "created_at": None,
        "updated_at": None,
    }

    record = OcrResultRepository._row_to_record(row)

    assert record.request_id == "req-1"
    assert record.candidates[0]["drug_info"]["item_seq"] == "ITEM-1"
    assert record.candidates[0]["evidence"]["match_method"] == "local_exact"
    assert record.candidates[0]["validation_messages"] == ["strength match"]
    assert record.normalized_names[0]["dosage"] == "500mg"
    assert record.pipeline_stages[0]["stage"] == "match"


def test_row_to_record_defaults_invalid_candidates_to_empty_list():
    row = {
        "request_id": "req-invalid",
        "raw_ocr_text": "invalid",
        "candidates": "{not-json",
        "decision_status": "NOT_FOUND",
        "match_confidence": 0.0,
    }

    record = OcrResultRepository._row_to_record(row)

    assert record.candidates == []
