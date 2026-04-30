# ChromaDB 운영 및 재생성 가이드

## 운영 원칙

- `chroma_db/`는 생성 데이터이므로 Git에 커밋하지 않는다.
- 운영 기본 방식은 Docker named volume 또는 이에 준하는 persistent volume이다.
- 재생성은 필요할 때 명시적으로 수행한다. 서버 시작 시마다 재생성하지 않는다.
- 현재 기준 활성 의약품 vector count 기대값은 `35,291`이다.

## Git 추적 확인

```powershell
git ls-files "AI/SilverLink-AI/chroma_db/*"
```

출력이 없어야 한다.

## 로컬 재생성

사전 조건:

- MySQL `silverlink` DB가 접근 가능해야 한다.
- `medications_master`에 활성 의약품 데이터가 적재되어 있어야 한다.
- `OPENAI_API_KEY`가 설정되어 있어야 한다.
- AI 가상환경이 준비되어 있어야 한다.

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m scripts.load_drug_data --chromadb-only --reset-chromadb
```

## Count 검증

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -c "from app.ocr.repository.drug_vector_repository import DrugVectorRepository; repo=DrugVectorRepository(); print(repo.collection_name, repo.persist_directory, repo.get_count())"
```

기대 결과:

- collection: `drug_embeddings`
- persist directory: `./chroma_db` 또는 운영 환경의 `CHROMA_PERSIST_DIRECTORY`
- count: `35,291`

활성 의약품 수가 바뀐 경우 count는 `medications_master`의 active row 수와 맞춰 판단한다.

## Docker Compose 운영

`docker-compose.yml`은 기본적으로 ChromaDB persistent volume을 사용한다.

```yaml
volumes:
  chroma_data:
```

서비스 mount:

```yaml
volumes:
  - chroma_data:/code/chroma_db
environment:
  - CHROMA_PERSIST_DIRECTORY=/code/chroma_db
```

운영에서 volume을 삭제하면 vector DB도 삭제된다. 삭제 후에는 재생성 명령을 다시 수행해야 한다.

## 운영 재생성 절차

1. API 서버가 현재 ChromaDB count를 확인한다.
2. count가 0이거나 기대 active count와 크게 다르면 maintenance window를 잡는다.
3. persistent volume을 유지한 상태에서 재생성 명령을 실행한다.
4. count 검증 명령으로 `drug_embeddings` count를 확인한다.
5. API 서버를 재시작하거나 vector matcher가 새 collection을 바라보는지 확인한다.

Docker 컨테이너 안에서 실행하는 예:

```powershell
docker compose exec api python -m scripts.load_drug_data --chromadb-only --reset-chromadb
docker compose exec api python -c "from app.ocr.repository.drug_vector_repository import DrugVectorRepository; print(DrugVectorRepository().get_count())"
```

## 장애 대응

- count가 `0`이면 OCR vector fallback은 동작하지 않고 MySQL/alias/local index 매칭만 사용된다.
- OpenAI embedding API 오류가 발생하면 `OPENAI_API_KEY`, 네트워크, rate limit을 확인한다.
- MySQL 조회가 실패하면 `RDS_HOST`, `RDS_PORT`, `RDS_USER`, `RDS_PASSWORD`, `RDS_DATABASE`를 확인한다.
- 운영 volume을 교체한 경우 재생성 완료 전까지 vector fallback 품질이 낮아질 수 있다.

## 다음 구현 계획

## Vector status endpoint

- AI endpoint: `GET /api/ocr/internal/vector/status`
- 인증: `X-SilverLink-Secret` 필수
- 응답 필드:
  - `collection_name`
  - `persist_directory`
  - `count`
  - `expected_count`
  - `embedding_model`
  - `status`: `READY`, `EMPTY`, `COUNT_MISMATCH`, `ERROR`
- `is_degraded`
- `checked_at`

상태 의미:

- `READY`: vector fallback 사용 가능
- `EMPTY`: collection이 비어 있음. 서버 시작은 계속되지만 vector fallback 품질 저하 가능
- `COUNT_MISMATCH`: 기대 count와 실제 count가 다름. 서버 시작은 계속되지만 재생성 여부 점검 필요
- `ERROR`: ChromaDB 상태 조회 실패. 서버 시작은 계속되지만 vector fallback 비정상 가능

FE는 이 endpoint를 직접 호출하지 않는다. BE admin proxy를 통해서만 노출한다.

## Startup warning

- AI startup 시 `DrugVectorRepository().get_count()`를 한 번 호출한다.
- count가 `0`이면 warning log를 남긴다.
- 기대 count가 설정되어 있고 실제 count와 다르면 warning log를 남긴다.
- warning은 서버 시작을 막지 않는다. OCR은 MySQL/alias fallback으로 계속 동작해야 한다.
- 환경 변수 후보:
  - `DRUG_VECTOR_EXPECTED_COUNT=35291`
  - `DRUG_VECTOR_STARTUP_CHECK=true`

## 다음 구현 후보

- BE proxy 후보: `GET /api/admin/ocr/vector-status`
- FE 후보: 관리자 AI/OCR 상태 화면에 count와 status 표시
## Phase 19 운영 점검 명령

Windows 로컬 실행은 항상 AI 가상환경의 Python을 명시한다.

OCR 품질 리포트 생성, alias 후보 upsert, 관리자 승인 절차는 `docs/ocr_ops.md`를 따른다.

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m pytest tests/unit_tests/test_drug_dictionary_index.py tests/unit_tests/test_medication_pipeline.py tests/unit_tests/test_ocr_owner_endpoint.py
.\.venv\Scripts\python.exe -m compileall -f app/ocr app/api app/core
```

ChromaDB count 확인:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -c "from app.ocr.repository.drug_vector_repository import DrugVectorRepository; repo=DrugVectorRepository(); print(repo.collection_name, repo.persist_directory, repo.get_count())"
```

AI 내부 vector status API 확인:

```powershell
curl.exe -H "X-SilverLink-Secret: <secret>" http://localhost:8000/api/ocr/internal/vector/status
```

BE 관리자 proxy 확인:

```powershell
curl.exe -H "Authorization: Bearer <admin-token>" http://localhost:8080/api/admin/ocr/vector-status
```

Dictionary reload 확인:

```powershell
curl.exe -X POST -H "X-SilverLink-Secret: <secret>" http://localhost:8000/api/ocr/admin/reload-dictionary
```

reload 응답에는 운영 확인용 필드가 포함된다.

- `success`
- `message`
- `elapsed_ms`
- `drug_count`
- `alias_count`
- `error_alias_count`
- `ngram_token_count`
- `existing_index_kept`

`success=false`이고 `existing_index_kept=true`이면 DB reload는 실패했지만 기존 메모리 인덱스는 계속 사용 중이라는 의미다. 이 경우 AI 서버 재시작 전까지 기존 alias 상태로 동작하므로 DB 연결과 alias 테이블 상태를 먼저 확인한 뒤 reload를 재시도한다.

Vector 검색 로그에는 `query`, `candidates`, `best_score`, `elapsed_ms`가 남는다. 운영에서 latency가 튀는 경우 embedding API 지연, ChromaDB volume 상태, MySQL 상세 조회 지연을 순서대로 확인한다.
