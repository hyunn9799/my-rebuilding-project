# OCR Quality Comparison

Generated at: not generated yet.

Run after generating before/after metrics:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m scripts.analyze_ocr_quality `
  --compare-before docs/ocr_quality_metrics_before.json `
  --compare-after docs/ocr_quality_metrics_after.json `
  --comparison-output docs/ocr_quality_comparison.md
```

## Summary

Pending generation from before/after metric JSON files.

## Decision Status Delta

Pending generation from before/after metric JSON files.

## Match Method Delta

Pending generation from before/after metric JSON files.

## Recommended Action Delta

Pending generation from before/after metric JSON files.

## Interpretation Notes

- Positive `MATCHED` delta is desirable when total OCR volume is comparable.
- Negative pending review delta is desirable for `LOW_CONFIDENCE`, `AMBIGUOUS`, and `NEED_USER_CONFIRMATION` combined.
- Recommended action counts should drop after safe aliases/rules are approved, unless new OCR volume introduced new patterns.
