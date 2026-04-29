# Admin Alias Management E2E

Purpose: verify the browser flow for reviewing OCR alias suggestions.

## Preconditions

- MySQL contains `medications_master` active rows.
- ChromaDB can be regenerated locally and is not committed to Git.
- AI API, Spring BE, and FE dev server can run locally.
- Test admin account exists: `admin_test / 1234`.

## 1. Seed a Pending Suggestion

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m scripts.seed_test_alias --alias-name 테스트_티이레놀_e2e
```

The script picks an active medication matching `타이레놀` first, then falls back to any active medication.

Optional arguments:

```powershell
.\.venv\Scripts\python.exe -m scripts.seed_test_alias `
  --alias-name 테스트_게보링_e2e `
  --suggestion-type error_alias `
  --item-name-like 게보린
```

## 2. Start Services

AI:

```powershell
cd AI\SilverLink-AI
poetry run uvicorn app.main:app --reload
```

Backend:

```powershell
cd BE\SilverLink-BE
powershell -ExecutionPolicy Bypass -File .\scripts\gradle-jdk21.ps1 bootRun
```

Frontend:

```powershell
cd FE\SilverLink-FE
npm.cmd run dev
```

## 3. Browser Checks

1. Open the FE dev URL.
2. Log in as `admin_test / 1234`.
3. Go to `/admin/alias-management`.
4. Confirm the seeded alias appears in the `PENDING` filter.
5. Click approve.
6. Confirm either:
   - success toast says the alias was approved and dictionary reloaded, or
   - warning toast says approval succeeded but dictionary reload failed.
7. Refresh the list and confirm the item no longer appears under `PENDING`.
8. Switch the filter to `APPROVED` and confirm the item appears.

Reject flow:

1. Seed a second alias with a different `--alias-name`.
2. Open `/admin/alias-management`.
3. Click reject.
4. Confirm the item leaves `PENDING`.
5. Switch the filter to `REJECTED` and confirm the item appears.

Reload button:

1. Click `사전 리로드`.
2. Confirm a success toast appears when AI reload succeeds.

## 4. DB Verification

Check suggestion status:

```sql
SELECT id, item_seq, alias_name, suggestion_type, review_status, reviewed_by, reviewed_at
FROM medication_alias_suggestions
WHERE alias_name LIKE '테스트\_%\_e2e'
ORDER BY id DESC;
```

Check approved alias target table:

```sql
SELECT item_seq, alias_name, alias_normalized, source, is_active
FROM medication_error_aliases
WHERE alias_name LIKE '테스트\_%\_e2e'
ORDER BY id DESC;
```

For `--suggestion-type alias`, check `medication_aliases` instead.

## 5. Automated Coverage

Backend proxy behavior is covered by:

```powershell
cd BE\SilverLink-BE
powershell -ExecutionPolicy Bypass -File .\scripts\gradle-jdk21.ps1 test --tests "com.aicc.silverlink.domain.ocr.controller.AdminAliasControllerTest"
```

This verifies:

- list requests proxy to AI with the internal secret header
- approve/reject requests encode `reviewed_by`
- reload requests proxy to the AI reload endpoint
