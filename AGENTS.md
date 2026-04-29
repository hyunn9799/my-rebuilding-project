# SilverLink Codex Harness

Use this repo map first. Do not scan the full tree unless the task requires it.

## Token Budget Rules

- Start with `docs/architecture-map.md`, then open only the matching skill under `.agents/skills/*/SKILL.md`.
- Prefer `rg`, `rg --files`, `Get-ChildItem`, and package/config files before reading source.
- Do not read generated, dependency, cache, secret, or upload-heavy paths unless the user explicitly asks.
- Avoid: `.git`, `node_modules`, `dist`, `build`, `target`, `.env`, `.venv`, `venv`, `__pycache__`, `.pytest_cache`, `.mypy_cache`, `AI/SilverLink-AI/chroma_db`, `AI/SilverLink-AI/mem_db`, `BE/SilverLink-BE/uploads`.
- For edits, identify the owning module first, then inspect the smallest related controller/service/repository/component set.
- Summarize findings before broadening scope.

## Repo Layout

- `FE/SilverLink-FE`: Vite + React + TypeScript admin/guardian/counselor/senior UI.
- `BE/SilverLink-BE`: Java 21 Spring Boot API with Gradle, JPA, Security, Redis, MySQL/H2, S3, Twilio.
- `AI/SilverLink-AI`: Python 3.12 FastAPI services for OCR, chatbot, callbot, SQS worker.
- `AI/OCR1.txt`: OCR reference/input artifact.

## Skill Routing

- OCR, medication extraction, drug alias/matching, LLM validation: `.agents/skills/ocr-drug-matching/SKILL.md`
- Spring API, auth/session/Redis/JPA/debugging/tests: `.agents/skills/spring-api-debug/SKILL.md`
- React routes, admin UI, API clients, layouts, shadcn/Tailwind: `.agents/skills/react-admin-ui/SKILL.md`
- Docker, SQS, S3, ECS-style deployment, service wiring: `.agents/skills/aws-ecs-architecture/SKILL.md`

## Common Commands

- FE: `cd FE/SilverLink-FE`; `npm run lint`; `npm run build`
- BE: `cd BE/SilverLink-BE`; `./gradlew test`; `./gradlew integrationTest`
- AI: `cd AI/SilverLink-AI`; `pytest`; `poetry run uvicorn app.main:app --reload`

## Verification Scope

- Frontend UI changes: lint/build when practical.
- Backend API changes: targeted Gradle test first, full `test` if shared behavior changes.
- AI pipeline changes: targeted `pytest tests/unit_tests/...` first, broader tests for service contracts.
