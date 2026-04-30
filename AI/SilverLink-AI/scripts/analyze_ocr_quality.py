"""Generate an OCR medication matching quality report from MySQL.

The script is read-only. It summarizes OCR decision statuses, matching methods,
and alias suggestion review state so repeated data-quality issues can be
promoted to normalization rules or alias/error-alias review tasks.
"""

from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from datetime import datetime
from pathlib import Path
from typing import Any

import pymysql

from app.core.config import configs

NORMALIZATION_PATTERNS = [
    (
        "spaced_or_ocr_zero_dosage",
        re.compile(r"\d(?:\s+[\doO]){1,5}\s*(?:mg|ml|mcg|g|iu|밀리그램|밀리리터|마이크로그램|그램)", re.IGNORECASE),
    ),
    ("excessive_spacing", re.compile(r"[\w가-힣]\s{2,}[\w가-힣]")),
]

MANUAL_REVIEW_METHODS = {"vector", "fuzzy", "ingredient", "ingredient_only"}


def _connect():
    return pymysql.connect(
        host=configs.RDS_HOST,
        port=configs.RDS_PORT,
        user=configs.RDS_USER,
        password=configs.RDS_PASSWORD,
        database=configs.RDS_DATABASE,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )


def _fetch_all(cursor, sql: str, params: tuple[Any, ...] = ()) -> list[dict[str, Any]]:
    cursor.execute(sql, params)
    return list(cursor.fetchall())


def _try_fetch_all(cursor, sql: str, params: tuple[Any, ...] = ()) -> list[dict[str, Any]]:
    try:
        return _fetch_all(cursor, sql, params)
    except Exception as exc:
        return [{"error": str(exc)}]


def _parse_json(value: Any) -> Any:
    if value is None or isinstance(value, (list, dict)):
        return value
    try:
        return json.loads(value)
    except Exception:
        return None


def _normalize_alias(value: str) -> str:
    return re.sub(r"\s+", "", value.strip().lower())


def _safe_int(value: Any, default: int = 1) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def _safe_float(value: Any, default: float = 0.0) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def _write_json(path: str | None, payload: Any) -> None:
    if not path:
        return
    output = Path(path)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {output}")


def _load_json(path: str) -> Any:
    return json.loads(Path(path).read_text(encoding="utf-8"))


def _raw_alias(value: Any) -> str:
    if not value:
        return ""
    for line in str(value).splitlines():
        line = line.strip()
        if line:
            return line[:200]
    return ""


def _candidate_drug(candidate: dict[str, Any]) -> dict[str, Any]:
    drug_info = candidate.get("drug_info") or {}
    if isinstance(drug_info, dict):
        return drug_info
    return {}


def _candidate_method(candidate: dict[str, Any]) -> str:
    evidence = candidate.get("evidence") or {}
    return str(candidate.get("method") or candidate.get("match_method") or evidence.get("match_method") or "").lower()


def _candidate_score(candidate: dict[str, Any]) -> float:
    try:
        return float(candidate.get("score") or candidate.get("match_score") or 0.0)
    except (TypeError, ValueError):
        return 0.0


def _normalization_reason(raw_text: str) -> str | None:
    for reason, pattern in NORMALIZATION_PATTERNS:
        if pattern.search(raw_text):
            return reason
    return None


def build_recommended_actions(metrics: dict[str, Any], min_frequency: int = 2) -> list[dict[str, Any]]:
    """Classify quality metrics into concrete follow-up actions."""
    actions: list[dict[str, Any]] = []
    seen: set[tuple[str, str, str]] = set()

    def add(action: dict[str, Any]) -> None:
        key = (
            str(action.get("action_type", "")),
            str(action.get("item_seq", "")),
            str(action.get("alias_normalized") or action.get("raw_text", "")),
        )
        if key in seen:
            return
        seen.add(key)
        actions.append(action)

    for raw_text, total in metrics.get("repeated_raw_texts", []):
        reason = _normalization_reason(str(raw_text))
        if reason and total >= min_frequency:
            add(
                {
                    "action_type": "normalization_candidate",
                    "reason": reason,
                    "raw_text": raw_text,
                    "frequency": total,
                }
            )

    for row in metrics.get("top_suggestions", []):
        if row.get("error"):
            continue
        suggestion_type = str(row.get("suggestion_type") or "error_alias")
        if suggestion_type not in {"alias", "error_alias"}:
            suggestion_type = "error_alias"
        frequency = _safe_int(row.get("frequency"))
        add(
            {
                "action_type": f"{suggestion_type}_candidate",
                "reason": "existing_alias_suggestion",
                "item_seq": row.get("item_seq"),
                "item_name": row.get("item_name"),
                "alias_name": row.get("alias_name"),
                "alias_normalized": row.get("alias_normalized") or _normalize_alias(str(row.get("alias_name") or "")),
                "frequency": frequency,
                "review_status": row.get("review_status"),
            }
        )

    for sample in metrics.get("pending_samples", []):
        if sample.get("error"):
            continue
        raw_alias = _raw_alias(sample.get("raw_ocr_text"))
        if not raw_alias:
            continue

        candidates = _parse_json(sample.get("candidates")) or []
        top_candidate = candidates[0] if candidates else {}
        drug = _candidate_drug(top_candidate)
        item_seq = sample.get("best_drug_item_seq") or drug.get("item_seq")
        item_name = sample.get("best_drug_name") or drug.get("item_name")
        method = _candidate_method(top_candidate)
        score = _candidate_score(top_candidate)

        if _normalization_reason(raw_alias):
            add(
                {
                    "action_type": "normalization_candidate",
                    "reason": _normalization_reason(raw_alias),
                    "request_id": sample.get("request_id"),
                    "raw_text": raw_alias,
                    "frequency": 1,
                }
            )
            continue

        if not item_seq:
            add(
                {
                    "action_type": "manual_review",
                    "reason": "no_stable_item_seq",
                    "request_id": sample.get("request_id"),
                    "raw_text": raw_alias,
                    "decision_status": sample.get("decision_status"),
                }
            )
            continue

        if method in MANUAL_REVIEW_METHODS or score < 0.5:
            add(
                {
                    "action_type": "manual_review",
                    "reason": f"unsafe_auto_alias_method={method or 'none'}",
                    "request_id": sample.get("request_id"),
                    "raw_text": raw_alias,
                    "item_seq": item_seq,
                    "item_name": item_name,
                    "score": score,
                }
            )
            continue

        add(
            {
                "action_type": "alias_candidate",
                "reason": f"pending_confirmation_method={method or 'unknown'}",
                "request_id": sample.get("request_id"),
                "item_seq": item_seq,
                "item_name": item_name,
                "alias_name": raw_alias,
                "alias_normalized": _normalize_alias(raw_alias),
                "frequency": 1,
            }
        )

    return actions


def _row_count_map(rows: list[dict[str, Any]], key_field: str, count_field: str = "total") -> dict[str, int]:
    counts: dict[str, int] = {}
    for row in rows:
        if row.get("error"):
            continue
        key = str(row.get(key_field) or "")
        if not key:
            continue
        counts[key] = counts.get(key, 0) + _safe_int(row.get(count_field), default=0)
    return counts


def _tuple_count_map(rows: list[list[Any]] | list[tuple[Any, Any]]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for row in rows:
        if len(row) < 2:
            continue
        key = str(row[0])
        counts[key] = counts.get(key, 0) + _safe_int(row[1], default=0)
    return counts


def _action_count_map(actions: list[dict[str, Any]]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for action in actions:
        key = str(action.get("action_type") or "")
        if not key:
            continue
        counts[key] = counts.get(key, 0) + 1
    return counts


def _diff_counts(before: dict[str, int], after: dict[str, int]) -> list[dict[str, Any]]:
    keys = sorted(set(before) | set(after))
    return [
        {
            "key": key,
            "before": before.get(key, 0),
            "after": after.get(key, 0),
            "delta": after.get(key, 0) - before.get(key, 0),
        }
        for key in keys
    ]


def compare_quality_metrics(before: dict[str, Any], after: dict[str, Any]) -> dict[str, Any]:
    before_decisions = _row_count_map(before.get("decision_counts", []), "decision_status")
    after_decisions = _row_count_map(after.get("decision_counts", []), "decision_status")
    before_methods = _tuple_count_map(before.get("match_method_counts", []))
    after_methods = _tuple_count_map(after.get("match_method_counts", []))
    before_actions = _action_count_map(before.get("recommended_actions", []))
    after_actions = _action_count_map(after.get("recommended_actions", []))

    matched_delta = after_decisions.get("MATCHED", 0) - before_decisions.get("MATCHED", 0)
    pending_keys = ("LOW_CONFIDENCE", "AMBIGUOUS", "NEED_USER_CONFIRMATION")
    pending_before = sum(before_decisions.get(key, 0) for key in pending_keys)
    pending_after = sum(after_decisions.get(key, 0) for key in pending_keys)

    return {
        "generated_at": datetime.now().isoformat(timespec="seconds"),
        "before_generated_at": before.get("generated_at"),
        "after_generated_at": after.get("generated_at"),
        "summary": {
            "matched_delta": matched_delta,
            "pending_review_delta": pending_after - pending_before,
            "recommended_action_delta": len(after.get("recommended_actions", [])) - len(before.get("recommended_actions", [])),
        },
        "decision_status_delta": _diff_counts(before_decisions, after_decisions),
        "match_method_delta": _diff_counts(before_methods, after_methods),
        "recommended_action_delta": _diff_counts(before_actions, after_actions),
    }


def collect_quality_metrics(limit: int = 20) -> dict[str, Any]:
    with _connect() as connection:
        with connection.cursor() as cursor:
            decision_counts = _try_fetch_all(
                cursor,
                """
                SELECT decision_status, COUNT(*) AS total
                FROM medication_ocr_results
                GROUP BY decision_status
                ORDER BY total DESC
                """,
            )
            pending_samples = _try_fetch_all(
                cursor,
                """
                SELECT request_id, decision_status, match_confidence,
                       best_drug_name, best_drug_item_seq, raw_ocr_text, candidates
                FROM medication_ocr_results
                WHERE decision_status IN ('LOW_CONFIDENCE', 'AMBIGUOUS', 'NEED_USER_CONFIRMATION')
                ORDER BY created_at DESC
                LIMIT %s
                """,
                (limit,),
            )
            suggestion_counts = _try_fetch_all(
                cursor,
                """
                SELECT review_status, suggestion_type, COUNT(*) AS total
                FROM medication_alias_suggestions
                GROUP BY review_status, suggestion_type
                ORDER BY total DESC
                """,
            )
            top_suggestions = _try_fetch_all(
                cursor,
                """
                SELECT s.review_status, s.suggestion_type, s.item_seq, m.item_name,
                       s.alias_name, s.alias_normalized, s.frequency
                FROM medication_alias_suggestions s
                LEFT JOIN medications_master m ON m.item_seq = s.item_seq
                ORDER BY s.frequency DESC, s.updated_at DESC
                LIMIT %s
                """,
                (limit,),
            )

    method_counts: Counter[str] = Counter()
    repeated_texts: Counter[str] = Counter()
    for sample in pending_samples:
        raw_text = sample.get("raw_ocr_text")
        if raw_text:
            repeated_texts[raw_text.strip()[:120]] += 1
        candidates = _parse_json(sample.get("candidates")) or []
        for candidate in candidates:
            method = candidate.get("method") or candidate.get("match_method")
            evidence = candidate.get("evidence") or {}
            method = method or evidence.get("match_method")
            if method:
                method_counts[str(method)] += 1

    metrics = {
        "generated_at": datetime.now().isoformat(timespec="seconds"),
        "decision_counts": decision_counts,
        "suggestion_counts": suggestion_counts,
        "top_suggestions": top_suggestions,
        "pending_samples": pending_samples,
        "match_method_counts": method_counts.most_common(),
        "repeated_raw_texts": repeated_texts.most_common(limit),
    }
    metrics["recommended_actions"] = build_recommended_actions(metrics)
    return metrics


def _table(headers: list[str], rows: list[list[Any]]) -> str:
    lines = [
        "| " + " | ".join(headers) + " |",
        "| " + " | ".join(["---"] * len(headers)) + " |",
    ]
    for row in rows:
        lines.append("| " + " | ".join(str(item).replace("\n", " ") for item in row) + " |")
    return "\n".join(lines)


def render_markdown(metrics: dict[str, Any]) -> str:
    parts = [
        "# OCR Quality Report",
        "",
        f"Generated at: `{metrics['generated_at']}`",
        "",
        "## Decision Status Counts",
        "",
        _table(
            ["decision_status", "total"],
            [[row.get("decision_status", "ERROR"), row.get("total", row.get("error", ""))] for row in metrics["decision_counts"]],
        ),
        "",
        "## Alias Suggestion Counts",
        "",
        _table(
            ["review_status", "suggestion_type", "total"],
            [
                [row.get("review_status", "ERROR"), row.get("suggestion_type", ""), row.get("total", row.get("error", ""))]
                for row in metrics["suggestion_counts"]
            ],
        ),
        "",
        "## Match Method Counts In Pending Samples",
        "",
        _table(["method", "total"], [[method, total] for method, total in metrics["match_method_counts"]]),
        "",
        "## Top Alias Suggestions",
        "",
        _table(
            ["status", "type", "item_seq", "item_name", "alias", "frequency"],
            [
                [
                    row.get("review_status", "ERROR"),
                    row.get("suggestion_type", ""),
                    row.get("item_seq", ""),
                    row.get("item_name", ""),
                    row.get("alias_name", row.get("error", "")),
                    row.get("frequency", ""),
                ]
                for row in metrics["top_suggestions"]
            ],
        ),
        "",
        "## Repeated Raw OCR Texts",
        "",
        _table(["raw_text_prefix", "total"], [[text, total] for text, total in metrics["repeated_raw_texts"]]),
        "",
        "## Recommended Actions",
        "",
        _table(
            ["action_type", "reason", "item_seq", "item_name", "alias/raw_text", "frequency"],
            [
                [
                    row.get("action_type", ""),
                    row.get("reason", ""),
                    row.get("item_seq", ""),
                    row.get("item_name", ""),
                    row.get("alias_name") or row.get("raw_text", ""),
                    row.get("frequency", ""),
                ]
                for row in metrics.get("recommended_actions", [])
            ],
        ),
        "",
        "## Classification Notes",
        "",
        "- Promote repeated spacing/unit/character cleanup issues to normalization rules.",
        "- Promote product-specific shorthand or OCR text tied to one item_seq to alias/error_alias review.",
        "- Keep vector-only, fuzzy-only, and ingredient-only matches in user confirmation flow.",
    ]
    return "\n".join(parts).strip() + "\n"


def render_comparison_markdown(comparison: dict[str, Any]) -> str:
    summary = comparison.get("summary", {})
    parts = [
        "# OCR Quality Comparison",
        "",
        f"Generated at: `{comparison['generated_at']}`",
        "",
        f"Before report: `{comparison.get('before_generated_at')}`",
        f"After report: `{comparison.get('after_generated_at')}`",
        "",
        "## Summary",
        "",
        _table(
            ["metric", "delta"],
            [
                ["MATCHED", summary.get("matched_delta", 0)],
                ["pending_review", summary.get("pending_review_delta", 0)],
                ["recommended_actions", summary.get("recommended_action_delta", 0)],
            ],
        ),
        "",
        "## Decision Status Delta",
        "",
        _table(
            ["decision_status", "before", "after", "delta"],
            [
                [row["key"], row["before"], row["after"], row["delta"]]
                for row in comparison.get("decision_status_delta", [])
            ],
        ),
        "",
        "## Match Method Delta",
        "",
        _table(
            ["method", "before", "after", "delta"],
            [
                [row["key"], row["before"], row["after"], row["delta"]]
                for row in comparison.get("match_method_delta", [])
            ],
        ),
        "",
        "## Recommended Action Delta",
        "",
        _table(
            ["action_type", "before", "after", "delta"],
            [
                [row["key"], row["before"], row["after"], row["delta"]]
                for row in comparison.get("recommended_action_delta", [])
            ],
        ),
        "",
        "## Interpretation Notes",
        "",
        "- Positive MATCHED delta is desirable when total OCR volume is comparable.",
        "- Negative pending_review delta is desirable for LOW_CONFIDENCE, AMBIGUOUS, and NEED_USER_CONFIRMATION combined.",
        "- Recommended action counts should drop after safe aliases/rules are approved, unless new OCR volume introduced new patterns.",
    ]
    return "\n".join(parts).strip() + "\n"


def alias_candidate_payload(actions: list[dict[str, Any]]) -> list[dict[str, Any]]:
    payload: list[dict[str, Any]] = []
    for action in actions:
        action_type = action.get("action_type")
        if action_type not in {"alias_candidate", "error_alias_candidate"}:
            continue
        item_seq = action.get("item_seq")
        alias_name = action.get("alias_name")
        if not item_seq or not alias_name:
            continue
        priority_score, priority_reason = alias_candidate_priority(action)
        payload.append(
            {
                "suggestion_type": "alias" if action_type == "alias_candidate" else "error_alias",
                "alias_name": alias_name,
                "alias_normalized": action.get("alias_normalized") or _normalize_alias(str(alias_name)),
                "item_seq": item_seq,
                "item_name": action.get("item_name"),
                "frequency": _safe_int(action.get("frequency")),
                "reason": action.get("reason"),
                "source_request_id": action.get("request_id"),
                "priority_score": priority_score,
                "priority_reason": priority_reason,
            }
        )
    return payload


def alias_candidate_priority(action: dict[str, Any]) -> tuple[int, str]:
    frequency = _safe_int(action.get("frequency"), default=1)
    action_type = str(action.get("action_type") or "")
    source_bonus = 20 if str(action.get("source") or "ocr_quality_report") == "ocr_quality_report" else 0
    type_bonus = 10 if action_type == "error_alias_candidate" else 0
    score_bonus = 5 if _safe_float(action.get("score"), default=1.0) >= 0.8 else 0
    priority_score = min(frequency, 20) * 3 + source_bonus + type_bonus + score_bonus

    reasons = [f"frequency={frequency}"]
    if source_bonus:
        reasons.append("source=ocr_quality_report")
    if type_bonus:
        reasons.append("type=error_alias")
    if score_bonus:
        reasons.append("score>=0.8")
    return priority_score, ", ".join(reasons)


def write_alias_candidates(path: str | None, actions: list[dict[str, Any]]) -> None:
    if not path:
        return
    _write_json(path, alias_candidate_payload(actions))


def upsert_alias_candidates(actions: list[dict[str, Any]]) -> int:
    candidates = alias_candidate_payload(actions)
    if not candidates:
        return 0

    with _connect() as connection:
        with connection.cursor() as cursor:
            for candidate in candidates:
                cursor.execute(
                    """
                    INSERT INTO medication_alias_suggestions (
                        item_seq, alias_name, alias_normalized,
                        suggestion_type, source, source_request_id,
                        review_status, frequency, is_active
                    ) VALUES (
                        %s, %s, %s, %s, 'ocr_quality_report', %s,
                        'PENDING', %s, 0
                    )
                    ON DUPLICATE KEY UPDATE
                        frequency = CASE
                            WHEN review_status = 'PENDING'
                                THEN frequency + VALUES(frequency)
                            ELSE frequency
                        END,
                        source_request_id = CASE
                            WHEN review_status = 'PENDING'
                                THEN VALUES(source_request_id)
                            ELSE source_request_id
                        END,
                        updated_at = CURRENT_TIMESTAMP
                    """,
                    (
                        candidate["item_seq"],
                        candidate["alias_name"],
                        candidate["alias_normalized"],
                        candidate["suggestion_type"],
                        candidate["source_request_id"],
                        candidate["frequency"],
                    ),
                )
        connection.commit()
    return len(candidates)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--limit", type=int, default=20)
    parser.add_argument("--output", default="docs/ocr_quality_report.md")
    parser.add_argument("--metrics-output", default=None)
    parser.add_argument("--alias-candidates-output", default=None)
    parser.add_argument("--compare-before", default=None)
    parser.add_argument("--compare-after", default=None)
    parser.add_argument("--comparison-output", default="docs/ocr_quality_comparison.md")
    parser.add_argument("--comparison-json-output", default=None)
    parser.add_argument(
        "--upsert-alias-candidates",
        action="store_true",
        help="Insert recommended alias/error_alias candidates into medication_alias_suggestions.",
    )
    args = parser.parse_args()

    if args.compare_before or args.compare_after:
        if not args.compare_before or not args.compare_after:
            parser.error("--compare-before and --compare-after must be used together.")
        comparison = compare_quality_metrics(_load_json(args.compare_before), _load_json(args.compare_after))
        output = Path(args.comparison_output)
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(render_comparison_markdown(comparison), encoding="utf-8")
        print(f"Wrote {output}")
        _write_json(args.comparison_json_output, comparison)
        return

    metrics = collect_quality_metrics(limit=args.limit)
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(render_markdown(metrics), encoding="utf-8")
    print(f"Wrote {output}")
    _write_json(args.metrics_output, metrics)
    write_alias_candidates(args.alias_candidates_output, metrics["recommended_actions"])
    if args.upsert_alias_candidates:
        count = upsert_alias_candidates(metrics["recommended_actions"])
        print(f"Upserted {count} alias candidate(s)")


if __name__ == "__main__":
    main()
