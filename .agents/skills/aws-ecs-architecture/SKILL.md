---
name: aws-ecs-architecture
description: Use for Docker, AWS-style deployment, ECS task/service design, S3, SQS worker/api separation, runtime env vars, networking, and production architecture changes.
---

# AWS ECS Architecture Skill

## Start Here

- Read `docs/architecture-map.md`.
- Inspect Docker/build/config files before source code.

## Primary Files

- AI compose: `AI/SilverLink-AI/docker-compose.yml`
- AI Dockerfiles: `AI/SilverLink-AI/Dockerfile`, `AI/SilverLink-AI/Dockerfile.worker`
- AI worker: `AI/SilverLink-AI/worker_main.py`, `app/queue/*`
- AI config: `AI/SilverLink-AI/app/core/config.py`
- BE Dockerfile: `BE/SilverLink-BE/Dockerfile`
- BE Gradle build: `BE/SilverLink-BE/build.gradle`
- BE runtime config: `BE/SilverLink-BE/src/main/resources/application*.yml`
- FE Vite config/env usage: `FE/SilverLink-FE/vite.config.ts`, `src/api/index.ts`

## Runtime Components

- Spring API: Java 21 jar, HTTP API on backend port, MySQL/Redis/S3/Twilio/WebClient integrations.
- FastAPI API: `uvicorn app.main:app --host 0.0.0.0 --port 8000`.
- SQS worker: `python worker_main.py`, consumes SQS, uses callbot/Luxia/Twilio/OpenAI and writes results back.
- Frontend: static Vite build served separately; uses `VITE_API_BASE_URL` and optional `VITE_AI_API_BASE_URL`.

## Env/Secret Areas

- AI envs: `OPENAI_API_KEY`, `LUXIA_API_KEY`, `TWILIO_SID`, `TWILIO_TOKEN`, `SILVERLINK_NUMBER`, `CALL_CONTROLL_URL`, `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `SQS_QUEUE_URL`, `SQS_DLQ_URL`, `MILVUS_URI`, `MILVUS_TOKEN`.
- FE envs: `VITE_API_BASE_URL`, `VITE_AI_API_BASE_URL`, `VITE_KAKAO_MAP_API_KEY`.
- Never open or print `.env` files unless the user explicitly asks and understands the risk.

## Search Recipes

- `rg -n "SQS|boto3|QUEUE|DLQ|AWS_|S3|Twilio|LUXIA|OPENAI" AI/SilverLink-AI BE/SilverLink-BE`
- `rg -n "server.port|spring.datasource|redis|s3|twilio|callbot|ai" BE/SilverLink-BE/src/main/resources`
- `rg -n "Dockerfile|docker-compose|uvicorn|worker_main|ENTRYPOINT|EXPOSE" AI/SilverLink-AI BE/SilverLink-BE`

## Work Rules

- Keep API and worker as separate runtime processes; do not merge long-running SQS work into request serving.
- Prefer env-driven configuration over hardcoded endpoints/secrets.
- Check health checks, startup order, and network assumptions before changing deployment structure.
- For ECS, model separate task definitions or services when scaling needs differ: FE, BE API, AI API, AI worker.

## Verification

- Validate builds at the source layer first: `npm run build`, `./gradlew build`, and AI dependency/test command as appropriate.
- For Docker changes, build the touched image locally when practical.
