---
name: ocr-drug-matching
description: Use for AI OCR medication extraction, drug dictionary matching, alias suggestions, validation confidence, and Spring/FE OCR integration.
---

# OCR Drug Matching Skill

## Start Here

- Read `docs/architecture-map.md` first.
- Then inspect only the OCR path relevant to the request.

## Primary Files

- AI API: `AI/SilverLink-AI/app/api/endpoints/ocr.py`
- Pipeline: `AI/SilverLink-AI/app/ocr/services/medication_pipeline.py`
- Matching: `text_normalizer.py`, `mysql_matcher.py`, `drug_dictionary_index.py`, `vector_matcher.py`, `rule_validator.py`, `pseudo_confidence_scorer.py`
- LLM: `llm_extractor.py`, `llm_descriptor.py`, `AI/SilverLink-AI/app/integration/llm/openai_client.py`
- Storage: `drug_repository.py`, `drug_vector_repository.py`, `ocr_result_repository.py`, `alias_suggestion_repository.py`
- Models/schemas: `app/ocr/model/*`, `app/ocr/schema/medication_schema.py`
- Spring proxy/admin: `BE/SilverLink-BE/src/main/java/com/aicc/silverlink/domain/ocr/**`
- FE screens/client: `FE/SilverLink-FE/src/pages/senior/SeniorOCR.tsx`, `src/pages/admin/AliasManagement.tsx`, `src/api/ocr.ts`

## Search Recipes

- `rg -n "MedicationPipeline|match|alias|confidence|OCRToken|NormalizedDrug" AI/SilverLink-AI/app/ocr AI/SilverLink-AI/tests`
- `rg -n "/api/ocr|alias-suggestions|ocr" BE/SilverLink-BE/src/main/java FE/SilverLink-FE/src`
- `rg -n "test_.*(ocr|mysql|pipeline|confidence|llm)" AI/SilverLink-AI/tests`

## Work Rules

- Do not open `chroma_db`, `mem_db`, or large local OCR artifacts unless explicitly requested.
- Preserve safety behavior: low-confidence OCR must not silently auto-confirm wrong medication, dosage, or manufacturer.
- For matching changes, update or add unit tests under `AI/SilverLink-AI/tests/unit_tests`.
- For API contract changes, check Spring DTO/proxy and FE API types together.

## Verification

- Targeted AI tests first, for example `pytest tests/unit_tests/test_medication_pipeline.py`.
- For Spring proxy changes, run the related Gradle controller/service test if one exists.
- For FE OCR UI changes, run `npm run lint` or `npm run build` when practical.
