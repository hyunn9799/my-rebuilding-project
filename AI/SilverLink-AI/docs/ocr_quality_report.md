# OCR Quality Report

Generated at: not generated yet.

Run:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m scripts.analyze_ocr_quality --output docs/ocr_quality_report.md
```

Export alias/error-alias candidates for review:

```powershell
.\.venv\Scripts\python.exe -m scripts.analyze_ocr_quality `
  --output docs/ocr_quality_report.md `
  --metrics-output docs/ocr_quality_metrics_before.json `
  --alias-candidates-output docs/ocr_alias_candidates.json
```

Optionally upsert exported candidates into `medication_alias_suggestions`:

```powershell
.\.venv\Scripts\python.exe -m scripts.analyze_ocr_quality `
  --output docs/ocr_quality_report.md `
  --metrics-output docs/ocr_quality_metrics_after.json `
  --alias-candidates-output docs/ocr_alias_candidates.json `
  --upsert-alias-candidates
```

Compare before/after metrics:

```powershell
.\.venv\Scripts\python.exe -m scripts.analyze_ocr_quality `
  --compare-before docs/ocr_quality_metrics_before.json `
  --compare-after docs/ocr_quality_metrics_after.json `
  --comparison-output docs/ocr_quality_comparison.md
```

## Decision Status Counts

Pending generation from MySQL.

## Alias Suggestion Counts

Pending generation from MySQL.

## Match Method Counts In Pending Samples

Pending generation from MySQL.

## Top Alias Suggestions

Pending generation from MySQL.

## Repeated Raw OCR Texts

Pending generation from MySQL.

## Recommended Actions

Pending generation from MySQL.

## Classification Notes

- Promote repeated spacing/unit/character cleanup issues to normalization rules.
- Promote product-specific shorthand or OCR text tied to one item_seq to alias/error_alias review.
- Keep vector-only, fuzzy-only, and ingredient-only matches in user confirmation flow.

See `docs/ocr_ops.md` for the full admin review and post-approval reload flow.
