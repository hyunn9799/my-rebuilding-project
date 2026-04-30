# OCR Operations Runbook

## Quality Report Loop

Generate a read-only OCR quality report and candidate export:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m scripts.analyze_ocr_quality `
  --output docs/ocr_quality_report.md `
  --metrics-output docs/ocr_quality_metrics_before.json `
  --alias-candidates-output docs/ocr_alias_candidates.json
```

Review `docs/ocr_quality_report.md` before writing to the database:

- `normalization_candidate`: promote only broad, low-risk OCR cleanup patterns to `TextNormalizer` rules.
- `alias_candidate`: use for stable product-name aliases tied to one `item_seq`.
- `error_alias_candidate`: use for stable OCR misreads tied to one `item_seq`.
- `manual_review`: keep vector, fuzzy, ingredient-only, or unstable candidates out of automatic upsert.

Upsert reviewed candidates into the admin approval queue:

```powershell
.\.venv\Scripts\python.exe -m scripts.analyze_ocr_quality `
  --output docs/ocr_quality_report.md `
  --metrics-output docs/ocr_quality_metrics_after.json `
  --alias-candidates-output docs/ocr_alias_candidates.json `
  --upsert-alias-candidates
```

The upsert writes `PENDING` rows to `medication_alias_suggestions` with source `ocr_quality_report`.
Existing approved or rejected suggestions keep their review status.

Compare before and after reports after approval, reload, or normalization changes:

```powershell
.\.venv\Scripts\python.exe -m scripts.analyze_ocr_quality `
  --compare-before docs/ocr_quality_metrics_before.json `
  --compare-after docs/ocr_quality_metrics_after.json `
  --comparison-output docs/ocr_quality_comparison.md
```

Use the comparison to confirm that `MATCHED` increased, pending review statuses decreased,
and recommended actions did not grow unexpectedly.

## Admin Review Loop

1. Open the admin Alias Management page.
2. Filter by `PENDING`.
3. Prioritize candidates with high frequency and source `ocr_quality_report`.
4. Use `source_request_id` to trace the OCR request when the candidate looks risky.
5. Approve only candidates that clearly map to one medication.
6. Reject ambiguous candidates, especially vector-only, fuzzy-only, and ingredient-only cases.
7. Run dictionary reload after approval if the automatic reload warning appears.

## Admin Quality Report Buttons

The admin Alias Management page also exposes manual operations:

- `품질 리포트 실행`: calls the BE admin proxy and runs the AI quality report in read-only mode.
- `alias 후보 등록`: requires a browser confirmation and writes only alias/error_alias candidates to `medication_alias_suggestions`.

These buttons are intentionally separate. Report generation is read-only; candidate registration writes to the admin approval queue.
Vector-only, fuzzy-only, ingredient-only, normalization, and manual-review actions are not upserted.

Each BE proxy call is stored in `ocr_quality_report_runs` and also emits an `AuditLog` record with compact metadata.
The admin page loads recent runs from:

```http
GET /api/admin/ocr/quality-report/runs?limit=10
```

Use the recent-run trend cards to check whether `MATCHED` is improving and whether pending/manual/alias candidates are shrinking across report runs.

## Post-Approval Checks

Check dictionary reload:

```powershell
curl -X POST http://localhost:8080/api/admin/alias-suggestions/reload-dictionary
```

Check vector fallback status:

```powershell
curl http://localhost:8080/api/admin/ocr/vector-status
```

Run targeted tests after promoting a new normalization rule:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m pytest tests/unit_tests/test_ocr_quality_regressions.py tests/unit_tests/test_medication_pipeline.py
```
