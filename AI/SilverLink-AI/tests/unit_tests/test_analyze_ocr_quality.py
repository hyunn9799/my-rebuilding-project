import json

from scripts.analyze_ocr_quality import (
    alias_candidate_payload,
    build_recommended_actions,
    compare_quality_metrics,
    render_comparison_markdown,
    render_markdown,
    write_alias_candidates,
    _write_json,
)


def test_build_recommended_actions_classifies_quality_issues():
    metrics = {
        "top_suggestions": [
            {
                "review_status": "PENDING",
                "suggestion_type": "error_alias",
                "item_seq": "A001",
                "item_name": "테스트정500mg",
                "alias_name": "테스트정 5OOmg",
                "alias_normalized": "테스트정5oomg",
                "frequency": 3,
            }
        ],
        "pending_samples": [
            {
                "request_id": "req-1",
                "decision_status": "NEED_USER_CONFIRMATION",
                "best_drug_item_seq": "A002",
                "best_drug_name": "샘플정500mg",
                "raw_ocr_text": "샘플정 500mg",
                "candidates": json.dumps(
                    [
                        {
                            "method": "prefix",
                            "score": 0.81,
                            "drug_info": {
                                "item_seq": "A002",
                                "item_name": "샘플정500mg",
                            },
                        }
                    ],
                    ensure_ascii=False,
                ),
            },
            {
                "request_id": "req-2",
                "decision_status": "LOW_CONFIDENCE",
                "raw_ocr_text": "타이레놀정 5 O O mg",
                "candidates": "[]",
            },
            {
                "request_id": "req-3",
                "decision_status": "AMBIGUOUS",
                "best_drug_item_seq": "A003",
                "best_drug_name": "성분후보정",
                "raw_ocr_text": "아세트아미노펜",
                "candidates": json.dumps(
                    [
                        {
                            "method": "vector",
                            "score": 0.7,
                            "drug_info": {
                                "item_seq": "A003",
                                "item_name": "성분후보정",
                            },
                        }
                    ],
                    ensure_ascii=False,
                ),
            },
        ],
        "repeated_raw_texts": [("타이레놀정 5 O O mg", 2)],
    }

    actions = build_recommended_actions(metrics)

    assert any(action["action_type"] == "normalization_candidate" for action in actions)
    assert any(action["action_type"] == "error_alias_candidate" for action in actions)
    assert any(
        action["action_type"] == "alias_candidate"
        and action["item_seq"] == "A002"
        and action["alias_normalized"] == "샘플정500mg"
        for action in actions
    )
    assert any(
        action["action_type"] == "manual_review"
        and action["reason"] == "unsafe_auto_alias_method=vector"
        for action in actions
    )


def test_alias_candidate_payload_and_json_export(tmp_path):
    actions = [
        {
            "action_type": "alias_candidate",
            "reason": "pending_confirmation_method=prefix",
            "request_id": "req-1",
            "item_seq": "A002",
            "item_name": "샘플정500mg",
            "alias_name": "샘플정 500mg",
            "alias_normalized": "샘플정500mg",
            "frequency": 2,
        },
        {
            "action_type": "normalization_candidate",
            "reason": "spaced_or_ocr_zero_dosage",
            "raw_text": "타이레놀정 5 O O mg",
            "frequency": 2,
        },
    ]

    payload = alias_candidate_payload(actions)
    output = tmp_path / "ocr_alias_candidates.json"
    write_alias_candidates(str(output), actions)

    assert payload == [
        {
            "suggestion_type": "alias",
            "alias_name": "샘플정 500mg",
            "alias_normalized": "샘플정500mg",
            "item_seq": "A002",
            "item_name": "샘플정500mg",
            "frequency": 2,
            "reason": "pending_confirmation_method=prefix",
            "source_request_id": "req-1",
            "priority_score": 31,
            "priority_reason": "frequency=2, source=ocr_quality_report, score>=0.8",
        }
    ]
    assert json.loads(output.read_text(encoding="utf-8")) == payload


def test_render_markdown_includes_recommended_actions():
    markdown = render_markdown(
        {
            "generated_at": "2026-04-30T10:00:00",
            "decision_counts": [],
            "suggestion_counts": [],
            "match_method_counts": [],
            "top_suggestions": [],
            "repeated_raw_texts": [],
            "recommended_actions": [
                {
                    "action_type": "normalization_candidate",
                    "reason": "spaced_or_ocr_zero_dosage",
                    "raw_text": "타이레놀정 5 O O mg",
                    "frequency": 2,
                }
            ],
        }
    )

    assert "## Recommended Actions" in markdown
    assert "spaced_or_ocr_zero_dosage" in markdown


def test_compare_quality_metrics_reports_before_after_deltas():
    before = {
        "generated_at": "2026-04-30T10:00:00",
        "decision_counts": [
            {"decision_status": "MATCHED", "total": 10},
            {"decision_status": "LOW_CONFIDENCE", "total": 4},
            {"decision_status": "AMBIGUOUS", "total": 2},
        ],
        "match_method_counts": [("prefix", 3), ("vector", 2)],
        "recommended_actions": [
            {"action_type": "alias_candidate"},
            {"action_type": "manual_review"},
            {"action_type": "manual_review"},
        ],
    }
    after = {
        "generated_at": "2026-04-30T11:00:00",
        "decision_counts": [
            {"decision_status": "MATCHED", "total": 13},
            {"decision_status": "LOW_CONFIDENCE", "total": 2},
            {"decision_status": "AMBIGUOUS", "total": 1},
        ],
        "match_method_counts": [["prefix", 4], ["vector", 1]],
        "recommended_actions": [
            {"action_type": "alias_candidate"},
        ],
    }

    comparison = compare_quality_metrics(before, after)
    markdown = render_comparison_markdown(comparison)

    assert comparison["summary"]["matched_delta"] == 3
    assert comparison["summary"]["pending_review_delta"] == -3
    assert comparison["summary"]["recommended_action_delta"] == -2
    assert "## Decision Status Delta" in markdown
    assert "| MATCHED | 10 | 13 | 3 |" in markdown


def test_write_json_creates_parent_directories(tmp_path):
    output = tmp_path / "nested" / "metrics.json"

    _write_json(str(output), {"decision_counts": []})

    assert json.loads(output.read_text(encoding="utf-8")) == {"decision_counts": []}
