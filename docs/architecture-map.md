# SilverLink Architecture Map

This map is intentionally compact. Use it before opening source files.

## High-Level System

- Frontend: `FE/SilverLink-FE`, Vite React TypeScript, React Router, TanStack Query, Axios, Tailwind/shadcn-style UI.
- Backend: `BE/SilverLink-BE`, Spring Boot 4.0.1, Java 21, Gradle, Spring Security, JPA, Redis, WebFlux/WebClient, S3, Twilio.
- AI service: `AI/SilverLink-AI`, FastAPI, dependency-injector, OpenAI/LangChain, Milvus/Chroma, MySQL, SQS worker, Twilio/Luxia integrations.
- Main data/API flow: React UI -> Spring `/api/**` -> MySQL/Redis/S3/external APIs; Spring proxies AI features to FastAPI; AI stores OCR/chatbot/callbot data and uses SQS for call work.

## Frontend Map

- Entry/routing: `src/main.tsx`, `src/App.tsx`.
- API client: `src/api/index.ts`; domain clients live in `src/api/*.ts`.
- Auth/session state: `src/contexts/*`, `src/hooks/useTokenRefresh.ts`, `src/hooks/useSessionTimeout.ts`, `src/components/auth/*`.
- Layout/navigation: `src/components/layout/DashboardLayout.tsx`, `src/config/*NavItems.tsx`.
- Role pages:
  - `src/pages/admin`: members, assignments, call test, AI stats, complaints, notices, policies, settings, welfare, alias management.
  - `src/pages/guardian`: dashboard, calls, welfare, inquiry, complaint, notices, profile, sensitive info, alerts.
  - `src/pages/counselor`: dashboard, seniors, calls, records, inquiries, alerts, notices, schedule requests.
  - `src/pages/senior`: login, dashboard, OCR, health, medication, notices, FAQ, profile, biometric.
- Env keys seen: `VITE_API_BASE_URL`, `VITE_AI_API_BASE_URL`, `VITE_KAKAO_MAP_API_KEY`.
- Avoid reading all `src/components/ui/*`; use only the specific component imported by the target page.

## Backend Map

- App entry: `src/main/java/com/aicc/silverlink/SilverLinkApplication.java`.
- Package root: `com.aicc.silverlink`.
- Domain folders: `admin`, `assignment`, `audit`, `auth`, `call`, `chatbot`, `complaint`, `consent`, `counseling`, `counselor`, `elderly`, `emergency`, `file`, `guardian`, `inquiry`, `map`, `medication`, `notice`, `notification`, `ocr`, `policy`, `session`, `system`, `terms`, `user`, `welfare`.
- Common layers: each domain usually contains `controller`, `service`, `repository`, `entity`, `dto`.
- Shared/global: `global/config`, `global/common`, `infra/external`, `infra/storage`, `infrastructure/callbot`.
- Main API roots include `/api/auth`, `/api/users`, `/api/admins`, `/api/admin/members`, `/api/elderly`, `/api/guardians`, `/api/counselors`, `/api/call-schedules`, `/api/call-details`, `/api/call-reviews`, `/api/ocr`, `/api/admin/alias-suggestions`, `/api/chatbot`, `/api/data/faqs`, `/api/data/inquiries`, `/api/notices`, `/api/notifications`, `/api/sse`, `/api/emergency-alerts`, `/api/map/facilities`, `/api/welfare`, `/api/policies`, `/api/admin/system-config`.
- Config files: `src/main/resources/application.yml`, `application-prod.yml`, `application-test.yml`, `application.properties`, `src/test/resources/application-ci.yml`.
- Tests: `src/test/java/...`; Gradle excludes `**/*IT.class` from default `test`; `integrationTest` includes `**/*IT.class`.

## AI Service Map

- App entry: `app/main.py`; router aggregate: `app/api/routes.py`.
- Endpoint modules: `app/api/endpoints/callbot.py`, `chatbot.py`, `ocr.py`.
- Dependency container: `app/core/container.py`; config: `app/core/config.py`; DI helpers: `app/core/middleware.py`.
- OCR pipeline:
  - API: `app/api/endpoints/ocr.py`
  - Services: `app/ocr/services/text_normalizer.py`, `mysql_matcher.py`, `drug_dictionary_index.py`, `vector_matcher.py`, `rule_validator.py`, `pseudo_confidence_scorer.py`, `llm_extractor.py`, `llm_descriptor.py`, `medication_pipeline.py`, `ocr_service.py`
  - Repositories: `app/ocr/repository/*`
  - Schemas/models: `app/ocr/schema/*`, `app/ocr/model/*`
- Chatbot: `app/chatbot/services/*`, `app/chatbot/repository/*`, `app/chatbot/schema/*`.
- Callbot/SQS: `app/callbot/*`, `app/queue/*`, `worker_main.py`.
- Integrations: `app/integration/llm/openai_client.py`, `tts/luxia_client.py`, `stt/clova_client.py`, `drug_api/drug_api_client.py`, `call.py`.
- Tests: `tests/unit_tests`, `tests/integration`, root `test_*.py`.
- Heavy/generated paths to avoid: `chroma_db`, `mem_db`, logs, local data artifacts.

## Deployment/Runtime Hints

- FE build uses Vite scripts in `package.json`.
- BE container copies `build/libs/*-SNAPSHOT.jar` into an Amazon Corretto 21 image.
- AI Dockerfile builds Poetry dependencies and runs `uvicorn app.main:app --host 0.0.0.0 --port 8000`.
- AI `docker-compose.yml` defines `api` and `worker` containers sharing env vars for OpenAI, Luxia, Twilio, AWS/SQS, and Milvus.

## Fast Search Recipes

- Find backend endpoint owner: `rg -n "@RequestMapping\\(\"/api/..." BE/SilverLink-BE/src/main/java`
- Find frontend route: `rg -n "path=\"/target|TargetPage" FE/SilverLink-FE/src`
- Find API client usage: `rg -n "apiClient|VITE_API_BASE_URL|/api/" FE/SilverLink-FE/src`
- Find AI route: `rg -n "@router\\.|APIRouter" AI/SilverLink-AI/app/api/endpoints`
- Find OCR logic: `rg -n "MedicationPipeline|MySQLMatcher|AliasSuggestion|OCRToken|NormalizedDrug" AI/SilverLink-AI/app AI/SilverLink-AI/tests`
