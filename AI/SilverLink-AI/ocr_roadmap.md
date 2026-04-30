# OCR 복약관리 로드맵

> 기준일: 2026-04-29  
> 환경: 로컬 MySQL `silverlink`, Redis, ChromaDB, JDK 21, AI `.venv` Python  
> API: `DrugPrdtPrmsnInfoService07` 의약품 허가정보  
> 목표: OCR 텍스트를 의약품 마스터, alias, vector DB로 검증하고 사용자 확인과 관리자 승인을 통해 매칭 품질을 개선한다.

---

## 1. 현재 진행상황

| 영역 | 상태 | 내용 |
| --- | --- | --- |
| AI OCR 파이프라인 | 완료 | LLM 추출, 정규화, MySQL/LocalDrugIndex 매칭, VectorDB fallback, 룰 검증, pseudo confidence |
| DB 스키마 | 완료 | `medications_master` 확장 컬럼, medication alias/suggestion/result 계열 테이블 |
| 의약품 마스터 | 완료 | `medications_master` 43,293건, 활성 35,291건, 취소 8,002건 |
| Alias 데이터 | 완료 | `medication_aliases` 94,737건, `medication_error_aliases` 86,715건 |
| 사용자 확인 API | 완료 | `validate-medication`, `confirm-medication`, `pending-confirmations`, requestId owner lookup |
| Alias suggestion | 완료 | 사용자 확정 시 `medication_alias_suggestions`에 `PENDING` 생성 확인 |
| 관리자 Alias API | 완료 | AI/BE 목록, 승인, 거부, dictionary reload 구현 및 API E2E 확인 |
| 관리자 Alias FE | 완료 | `/admin/alias-management` 화면, 필터, 승인/거부, reload 버튼 구현 |
| ChromaDB | 완료 | 활성 의약품 35,291건 전체 적재, 컬렉션 `drug_embeddings`, 운영 기본값 persistent volume |
| 데이터 파이프라인 | 완료 | API 적재와 `--chromadb-only` 임베딩 재생성 분리 |
| Backend 보안 | 완료 | `/api/ocr/**` 인증 필수, 어르신별 OCR 권한 검사, requestId owner lookup 기반 confirm 권한 검사 |
| Backend 빌드 | 완료 | JDK 21 기준 Gradle 빌드/테스트 성공, `admin_test / 1234`는 prod profile 제외 |
| Frontend 빌드 | 완료 | `npm.cmd run build` 성공, 경고는 `docs/warnings-todo.md`로 분리 |
| 문서/하네스 | 진행 중 | `AGENTS.md`, `.agents/skills/*`, `docs/architecture-map.md`, `docs/warnings-todo.md` |

---

## 2. 검증 완료 내역

### AI

- `tests/unit_tests`: `62 passed, 2 skipped`
- `app/api/endpoints/ocr.py`, `drug_dictionary_index.py`, `scripts/load_drug_data.py` compile 확인 대상
- 실제 MySQL 연결 확인
- Alias suggestion 관리자 API 직접 검증
- 사용자 confirm 흐름 검증
- ChromaDB 전체 적재 후 count `35,291` 확인
- LLM descriptor에서 공식 효능 정보가 없을 때 효능을 유추하지 않도록 fallback 문구 적용
- requestId owner lookup API 단위 테스트 추가
- `/api/ocr/admin/*`, `/api/ocr/internal/*` secret 검증 적용
- `docs/chromadb_ops.md`에 persistent volume 운영, 재생성, count 검증 절차 문서화
- vector status endpoint와 startup warning 추가

### Backend

- `BE/SilverLink-BE/scripts/gradle-jdk21.ps1` 추가
- `SessionServiceTest`를 현재 Redis pipeline/HMGET/Lua varargs 동작에 맞게 수정
- `AdminAliasControllerTest` 추가
- `OcrProxyControllerTest` 추가
- AI/BE 서버 기동 후 관리자 토큰 기반 alias 목록/승인/거부/reload E2E 확인
- `./scripts/gradle-jdk21.ps1 test`: `BUILD SUCCESSFUL`
- AI OCR 4xx 오류 status 보존 테스트 추가

### Frontend

- `SeniorOCR.tsx` confirm 모달과 새 API 필드 표시
- `SeniorDashboard.tsx` 미확인 건수 배지
- `AliasManagement.tsx` 관리자 alias 검토 화면
- `adminNavItems.tsx` 관리자 메뉴
- `aliasAdmin.ts`와 BE endpoint 파라미터 정합성 확인
- `npm.cmd run build` 성공

---

## 3. 데이터 기준

| 항목 | 값 |
| --- | --- |
| MySQL DB | `silverlink` |
| 전체 의약품 | 43,293건 |
| 활성 의약품 | 35,291건 |
| 취소 의약품 | 8,002건 |
| Alias | 94,737건 |
| Error alias | 86,715건 |
| Vector DB | ChromaDB PersistentClient |
| Vector 경로 | `AI/SilverLink-AI/chroma_db/` |
| Vector 컬렉션 | `drug_embeddings` |
| Vector 적재 건수 | 35,291건 |
| Embedding 모델 | `text-embedding-3-small` |

`chroma_db/`는 생성 데이터다. Git 추적에서는 제외하고, 운영에서는 persistent volume 또는 별도 artifact로 관리하는 방향이 적합하다.

---

## 4. 앞으로 할 작업

## Phase 12. Vector fallback 테스트 보강 - 완료

우선순위: 높음  
예상 소요: 2~4시간

완료:

1. `MedicationPipeline`에서 MySQL/LocalDrugIndex 저신뢰 또는 실패 시 `VectorMatcher` 호출 조건 확인
2. 벡터 후보의 score/distance가 confidence에 반영되는 방식 정리
3. 단위 테스트 추가
   - 상품명 일부 OCR
   - 성분명만 OCR
   - 제조사 포함 OCR
   - alias에는 없지만 vector로 가까운 후보가 있는 경우
   - 무관한 OCR 텍스트에서 낮은 confidence 유지
4. vector-only 후보는 자동 확정되지 않고 사용자 확인으로 가는지 검증
5. 무관한 vector-only 후보는 `LOW_CONFIDENCE` 유지하도록 분기 보강

검증:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m pytest tests/unit_tests/test_medication_pipeline.py
.\.venv\Scripts\python.exe -m pytest tests/unit_tests
```

## Phase 13. 관리자 Alias 브라우저 E2E - 완료

우선순위: 높음  
예상 소요: 2~3시간

완료:

1. 테스트 alias seed script 보강
2. `/admin/alias-management` 수동 E2E 절차 문서화
3. 목록 노출, 승인, 거부, dictionary reload 버튼 검증 절차 정리
4. BE admin alias 프록시 reviewer query encoding 보강
5. `AdminAliasControllerTest`로 목록/승인/거부/reload 프록시 검증

산출물:

- `AI/SilverLink-AI/docs/admin_alias_e2e.md`
- `AI/SilverLink-AI/scripts/seed_test_alias.py`

## Phase 14. 보안/권한 정리 - 완료

우선순위: 높음  
예상 소요: 2~4시간

완료:

1. `admin_test / 1234` seed를 prod profile에서 실행되지 않도록 제한
2. `/api/admin/alias-suggestions/**`에 `hasRole('ADMIN')` 이중 잠금 추가
3. `/api/ocr/**` 공개 허용 제거 및 인증 필수화
4. `/api/ocr/validate-medication` 어르신/보호자/상담사/관리자 접근 권한 확인
5. `/api/ocr/pending-confirmations/{elderlyUserId}` 어르신/보호자/상담사/관리자 접근 권한 확인
6. `/api/ocr/confirm-medication`은 인증/역할 검사를 적용했고, requestId만으로는 본인/위임 여부를 판정할 수 없어 요청 계약 확장이 필요함

## Phase 15. BE OCR 프록시 보강

우선순위: 높음  
상태: 진행 중  
예상 소요: 2~4시간

목표:

- FE가 AI 서버를 직접 호출하지 않고 Spring Boot BE를 통해 OCR API를 호출하도록 정리
- `/api/ocr/validate-medication`, `/api/ocr/confirm-medication`, `/api/ocr/pending-confirmations`의 인증/인가 책임을 BE에 둠
- `/api/ocr/admin/reload-dictionary`는 ADMIN 전용으로 제한

### Phase 15A. requestId 소유자 조회 API 추가 - 완료

우선순위: 높음  
예상 소요: 2~3시간

문제:

- BE의 `/api/ocr/confirm-medication`은 현재 `requestId`, `selectedItemSeq`, `confirmed`만 받는다.
- requestId가 어느 어르신의 OCR 결과인지 BE가 알 수 없어, 인증 사용자가 해당 confirm을 수행할 권한이 있는지 완전히 검증할 수 없다.
- AI에는 `medication_ocr_results.elderly_user_id`가 이미 저장되어 있으므로, requestId 소유자 조회를 AI 내부 API로 제공하면 BE에서 기존 Phase 14 권한 검사 로직을 재사용할 수 있다.

구현:

1. AI FastAPI
   - `GET /api/ocr/internal/results/{request_id}/owner`
   - 응답 200:
     ```json
     {
       "request_id": "uuid",
       "elderly_user_id": 10,
       "decision_status": "NEED_USER_CONFIRMATION",
       "user_confirmed": null
     }
     ```
   - 404: requestId 없음
   - 409: 이미 confirm 처리된 requestId
   - 422: `elderly_user_id`가 없는 OCR 결과
   - 인증: BE가 AI 호출 시 사용하는 `X-SilverLink-Secret` 헤더 필수

2. AI schema/repository
   - `OcrResultOwnerResponse` schema 추가
   - `OcrResultRepository.get_owner_by_request_id(request_id)` 추가
   - 조회 컬럼은 `request_id`, `elderly_user_id`, `decision_status`, `user_confirmed`로 제한
   - candidates/raw OCR 원문은 반환하지 않음

3. Spring BE
   - `ConfirmMedicationRequest` DTO는 변경하지 않음
   - `OcrProxyController.confirmMedication`에서 AI owner API를 먼저 호출
   - owner 응답의 `elderlyUserId`로 `assertCanReadElderlyOcr(elderlyUserId)` 실행
   - 권한 통과 후 기존 AI `/api/ocr/confirm-medication` 호출
   - AI owner API가 404/409/422를 반환하면 BE도 동일 계열 상태로 변환

4. 테스트
   - AI unit: requestId 소유자 조회 성공, 없음, 이미 confirm됨, elderly_user_id 없음
   - BE unit: 미연결 보호자가 다른 어르신 requestId confirm 시 403
   - BE unit: 배정 상담사가 requestId confirm 시 AI confirm endpoint 호출
   - BE unit: owner API가 409면 confirm endpoint를 호출하지 않음

5. 운영 보안
   - owner API는 FE에서 직접 호출하지 않는다.
   - AI 라우터에서 secret header 검증 dependency를 공통화한다.
   - 이후 `/api/ocr/admin/*`, `/api/ocr/internal/*`도 같은 internal secret 검증을 적용한다.

검증:

- AI unit 전체: 62 passed, 2 skipped
- BE Gradle test 전체: BUILD SUCCESSFUL

### Phase 15B. AI 내부 endpoint secret 적용 범위 확장 - 완료

우선순위: 높음  
예상 소요: 1~2시간

목표:

- FE/외부 클라이언트가 AI 관리자 또는 내부 OCR endpoint를 직접 호출하지 못하게 한다.
- Spring BE만 `X-SilverLink-Secret`으로 AI 내부/관리자 endpoint를 호출하도록 경계를 명확히 한다.

완료:

1. `verify_internal_secret` dependency를 OCR router 내 공통 helper로 유지
2. AI `/api/ocr/admin/alias-suggestions` GET에 secret 검증 적용
3. AI `/api/ocr/admin/alias-suggestions/{id}/approve` PUT에 secret 검증 적용
4. AI `/api/ocr/admin/alias-suggestions/{id}/reject` PUT에 secret 검증 적용
5. AI `/api/ocr/admin/reload-dictionary` POST에 secret 검증 적용
6. BE `AdminAliasController`가 기존 secret header를 계속 전달하는지 테스트로 고정
7. 잘못된 secret/missing secret에 대해 AI unit test 추가

검증:

- AI `test_ocr_owner_endpoint.py`: 7 passed
- BE `AdminAliasControllerTest`: BUILD SUCCESSFUL

### Phase 15C. BE OCR proxy 오류 응답 정리 - 완료

우선순위: 중간  
예상 소요: 1~2시간

문제:

- `validate-medication`, `confirm-medication`, `pending-confirmations`에서 AI 오류를 일부 `500`으로 뭉뚱그려 반환한다.
- owner lookup은 404/409/422를 보존하지만, confirm 본 호출 오류는 아직 일반 실패 응답으로 감싼다.

완료:

1. `RestClientResponseException` 처리 공통 helper 검토
2. AI 4xx 오류 응답은 BE에서도 동일 status로 반환
3. confirm 본 호출에서 AI 4xx는 동일 status로 반환
4. pending 조회에서 AI 4xx와 일반 500 오류를 구분
5. `OcrProxyControllerTest`에 owner/confirm 오류 매핑 케이스 추가

검증:

- BE `OcrProxyControllerTest`: BUILD SUCCESSFUL

### Phase 15 완료 기준

- AI `/api/ocr/internal/*`와 `/api/ocr/admin/*`는 secret header 없이는 접근 불가
- BE `/api/ocr/**`는 인증 필수
- BE confirm은 requestId owner lookup 후 어르신/보호자/상담사/관리자 권한을 확인
- AI 4xx 오류는 BE에서 의미 있는 status로 보존
- AI unit 전체: 53 passed, 2 skipped
- BE Gradle test 전체: BUILD SUCCESSFUL

## Phase 16. ChromaDB 운영 정책 결정 - 완료

우선순위: 높음  
예상 소요: 1~2시간

목표:

- Git에 포함하지 않는 ChromaDB를 로컬/운영에서 재현 가능하게 만든다.
- 배포 시 vector DB가 비어 있거나 schema가 맞지 않는 상태를 빠르게 감지한다.

완료:

1. `chroma_db/` Git 추적 제외 상태 재확인
2. ChromaDB 재생성 명령을 `AI/SilverLink-AI/docs/chromadb_ops.md`로 문서화
3. 로컬 재생성 절차와 예상 count `35,291` 기록
4. 운영 기본 방식을 persistent volume으로 결정
5. `docker-compose.yml`에 `chroma_data:/code/chroma_db` named volume 추가
6. API/worker 서비스에 `CHROMA_PERSIST_DIRECTORY=/code/chroma_db` 반영
7. vector status endpoint와 startup warning은 Phase 16A로 분리

검증:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m scripts.load_drug_data --chromadb-only --reset-chromadb
.\.venv\Scripts\python.exe -c "from app.ocr.repository.drug_vector_repository import DrugVectorRepository; print(DrugVectorRepository().get_count())"
```

주의:

- `git ls-files "AI/SilverLink-AI/chroma_db/*"` 출력은 없어야 한다.
- 현재 `AI/SilverLink-AI/mem_db/meta.json`은 별도 generated data 정리 후보로 남아 있다.

### Phase 16A. Vector status endpoint 및 startup warning - 완료

우선순위: 높음  
예상 소요: 2~3시간

목표:

- ChromaDB가 비어 있거나 기대 count와 다른 상태를 API/로그로 빠르게 감지한다.
- 운영에서 persistent volume이 잘못 마운트되거나 삭제된 경우 OCR 품질 저하를 즉시 알 수 있게 한다.

완료:

1. AI config 추가
   - `DRUG_VECTOR_EXPECTED_COUNT=35291`
   - `DRUG_VECTOR_STARTUP_CHECK=true`
   - `SILVERLINK_INTERNAL_SECRET`
   - `DRUG_VECTOR_EXPECTED_COUNT`가 없거나 `0`이면 expected count 비교 생략
2. AI schema 추가
   - `VectorStatusResponse`
   - fields: `collection_name`, `persist_directory`, `count`, `expected_count`, `embedding_model`, `status`, `message`, `is_degraded`, `checked_at`
3. `DrugVectorRepository` 보강
   - `get_status(expected_count: int | None)` 메서드 추가
   - status 규칙:
     - `READY`: count > 0이고 expected_count 미설정 또는 일치
     - `EMPTY`: count == 0
     - `COUNT_MISMATCH`: expected_count 설정 및 count 불일치
     - `ERROR`: ChromaDB 초기화/조회 실패
4. AI internal endpoint 추가
   - `GET /api/ocr/internal/vector/status`
   - `X-SilverLink-Secret` 필수
   - FE 직접 호출 금지, BE proxy 전용 internal endpoint
   - ChromaDB 상태가 `ERROR`여도 HTTP 200 + `status=ERROR` payload 반환
5. startup warning 추가
   - FastAPI startup hook 또는 lifespan에서 vector status 1회 확인
   - `EMPTY`, `COUNT_MISMATCH`, `ERROR`면 warning log
   - 서버 시작은 차단하지 않음
   - startup check 자체가 실패해도 warning만 남기고 계속 시작
6. BE proxy 계획
   - `GET /api/admin/ocr/vector-status`
   - ADMIN 전용
   - AI secret header로 internal endpoint 호출
7. 테스트
   - AI unit: READY/EMPTY/COUNT_MISMATCH/ERROR status 판정
   - AI unit: internal secret 누락/불일치 시 401
   - AI unit: secret 정상 시 status response 반환
   - BE unit: admin vector-status proxy가 secret header 전달은 Phase 16B에서 수행

검증:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m pytest tests/unit_tests -q
```

결과: 62 passed, 2 skipped

추가 검증 계획:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m compileall -f app/ocr app/api app/core
```

결과:

- `test_drug_dictionary_index.py`, `test_medication_pipeline.py`, `test_ocr_owner_endpoint.py`: 32 passed
- `compileall`: 성공

결과: compileall 성공

완료 보고 체크리스트:

1. 수정/추가 파일 목록
2. config 추가 내용
3. `VectorStatusResponse` schema 필드
4. `get_status` 상태 판정 방식
5. internal endpoint 경로와 secret 검증 방식
6. startup warning 동작 방식
7. 테스트 및 compile 결과
8. `chromadb_ops.md` 업데이트 여부
9. 남은 TODO

BE proxy는 다음 단계로 분리한다.

### Phase 16B. BE admin vector-status proxy - 완료

우선순위: 중간  
예상 소요: 1~2시간

목표:

- FE가 AI internal endpoint를 직접 호출하지 않도록 BE admin endpoint로만 vector 상태를 노출한다.

완료:

1. BE DTO 추가
   - `VectorStatusResponse`
   - AI 응답 snake_case mapping
2. BE admin endpoint 추가
   - `GET /api/admin/ocr/vector-status`
   - `hasRole('ADMIN')`
3. BE가 AI `/api/ocr/internal/vector/status` 호출
   - `X-SilverLink-Secret` header 전달
4. AI 4xx/5xx 오류 status 보존
5. controller test 추가
   - secret header 전달
   - 200 응답 mapping
   - AI 401/500 오류 mapping

검증:

```powershell
cd BE\SilverLink-BE
powershell -ExecutionPolicy Bypass -File .\scripts\gradle-jdk21.ps1 test --tests "com.aicc.silverlink.domain.ocr.controller.*"
```

결과: BUILD SUCCESSFUL

## Phase 17. OCR 후보 테이블 정합성 점검 - 완료

우선순위: 중간  
예상 소요: 1~2시간

결정:

- 현재는 별도 `medication_ocr_candidates` 테이블을 만들지 않는다.
- OCR 후보는 `medication_ocr_results.candidates` JSON 컬럼을 공식 저장소로 유지한다.
- 후보별 검색/통계/감사 요구가 커질 때 별도 후보 테이블을 재검토한다.

완료:

1. `medication_ocr_candidates` 참조 여부 검색
   - 실제 schema/repository/controller 경로에서 별도 후보 테이블 구현 없음
   - `medication_ocr_results.candidates` JSON 저장/조회 경로만 사용 중
2. JSON 저장 정책 확정
   - 저장 대상: `drug_info`, `score`, `method`, `evidence`, `validation_messages`
   - pending-confirmations API와 FE 후보 확인 UI는 이 JSON 후보 목록을 그대로 사용
3. 단위 테스트 추가
   - `tests/unit_tests/test_ocr_result_repository.py`
   - candidates JSON 복원
   - 잘못된 candidates JSON은 빈 배열로 fallback

검증:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m pytest tests/unit_tests/test_ocr_result_repository.py
```

결과:

- `test_ocr_result_repository.py`: 2 passed
- 관련 OCR 회귀 확인: `test_ocr_result_repository.py`, `test_medication_pipeline.py`, `test_ocr_owner_endpoint.py` 총 23 passed

## Phase 18. 사용자 복약 등록까지 E2E - 완료

우선순위: 중간  
예상 소요: 3~5시간

완료:

1. Senior OCR 화면에서 OCR 실행
   - `validate-medication` 응답의 `request_id`를 FE 상태로 유지
2. 후보 확인 모달에서 선택
   - 사용자 확정 성공 시 `confirmedOcrRequestId`에 requestId 저장
3. `confirmMedication` 호출 확인
   - FE가 BE `/api/ocr/confirm-medication` 호출
   - BE가 requestId owner lookup 기반 권한 검사 후 AI confirm endpoint 호출
4. 복약 일정 등록 다이얼로그 필드 매핑 확인
   - OCR 확정 후 등록하는 복약 일정 요청에 `sourceOcrRequestId` 포함
5. medication schedule 저장 API 확인
   - `MedicationRequest.sourceOcrRequestId` 추가
   - `MedicationSchedule.sourceOcrRequestId` 컬럼 추가
   - `MedicationResponse.sourceOcrRequestId` 응답 추가
6. dashboard 미확인 badge 갱신 확인
   - confirm 성공 후 requestId 기반으로 pending 대상에서 제외되는 기존 흐름 유지

구현 메모:

- 기존 `MedicationOcrLog`/`sourceOcrLog`는 파일 업로드 기반 OCR 로그 용도로 유지한다.
- AI OCR 결과 UUID는 `source_ocr_request_id`에 직접 저장한다.
- 복약 등록 자체는 기존 `POST /api/medications` 흐름을 유지하고, OCR 출처는 optional field로만 확장한다.

검증:

```powershell
cd BE\SilverLink-BE
powershell -ExecutionPolicy Bypass -File .\scripts\gradle-jdk21.ps1 test --tests "com.aicc.silverlink.domain.medication.service.MedicationServiceTest"

cd FE\SilverLink-FE
npm.cmd run build
```

결과:

- BE `MedicationServiceTest`: BUILD SUCCESSFUL
- FE `npm.cmd run build`: 성공

## Phase 19. 성능/운영 안정화 - 완료

우선순위: 중간  
예상 소요: 2~4시간

완료:

1. `reload-dictionary` 실행 시간 측정
   - `DrugDictionaryIndex.last_reload_stats`에 `elapsed_ms` 기록
   - AI reload endpoint 응답에 `elapsed_ms`, `drug_count`, `alias_count`, `error_alias_count`, `ngram_token_count` 포함
2. reload 중 atomic swap 동작 확인
   - reload 실패 시 기존 `_current` 인덱스를 유지
   - 실패 응답에 `existing_index_kept=true`와 기존 count 지표 반환
3. ChromaDB 검색 latency 측정
   - `VectorMatcher.match()` 로그에 `query`, `candidates`, `best_score`, `elapsed_ms` 기록
4. optional dependency 경고 정리
   - FE build의 Browserslist/chunk 경고는 기능 실패가 아닌 운영 문서/빌드 경고로 유지
5. Windows 로컬 실행 문서에 `.venv\Scripts\python.exe` 사용 명시
   - `docs/chromadb_ops.md`에 count, vector status, reload 확인 명령 추가

검증:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m pytest tests/unit_tests/test_drug_dictionary_index.py tests/unit_tests/test_medication_pipeline.py tests/unit_tests/test_ocr_owner_endpoint.py
.\.venv\Scripts\python.exe -m compileall -f app/ocr app/api app/core
```

## Phase 20. 데이터 품질 개선 - 완료

우선순위: 낮음~중간  
예상 소요: 2~6시간

완료:

1. 품질 분석 리포트 스크립트 추가
   - `scripts/analyze_ocr_quality.py`
   - `medication_ocr_results` 결정 분포, 보류 샘플, 후보 match method, 반복 raw text를 집계
   - `alias_suggestions` review status/type 분포와 빈도 높은 제안을 집계
2. 운영 리포트 템플릿 추가
   - `docs/ocr_quality_report.md`
   - 로컬/운영 DB 연결 후 실행할 명령과 확인 항목을 문서화
3. 반복 OCR 오류 정규화 규칙 추가
   - `5 O O mg`, `5 o o mg`처럼 OCR이 0을 O/o로 분리한 용량을 `500mg`으로 복원
   - 기존 숫자 공백 결합 규칙보다 먼저 적용해 용량 단위가 붙은 안전한 패턴만 처리
4. 성분명 기반 후보 UX 확인
   - FE OCR 후보 카드가 이미 성분명, 제조사, match method, 강도 불일치, validation message를 노출 중임을 확인
   - 추가 UI 변경보다 품질 리포트로 실제 성분명 기반 보류 케이스를 먼저 수집하는 방향으로 정리
5. 회귀 테스트 추가
   - `tests/unit_tests/test_ocr_quality_regressions.py`
   - `타이레놀정 5 O O mg` 정규화 결과가 약명 `타이레놀정`, 용량 `500mg`으로 분리되는지 검증

검증:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m pytest tests/unit_tests/test_ocr_quality_regressions.py tests/unit_tests/test_medication_pipeline.py tests/unit_tests/test_drug_dictionary_index.py tests/unit_tests/test_ocr_owner_endpoint.py
.\.venv\Scripts\python.exe -m compileall -f app/ocr app/api app/core scripts/analyze_ocr_quality.py
```

참고:

- Phase 21에서 `scripts/seed_aliases.py`의 기존 들여쓰기 오류를 정리해 `.\.venv\Scripts\python.exe -m compileall -f scripts`가 통과하도록 복구했다.

## Phase 21. 운영 품질 루프 자동화 - 완료

우선순위: 중간  
예상 소요: 3~6시간

완료:

1. `analyze_ocr_quality.py` 실행 결과를 기준으로 승인 alias와 error alias 후보를 분리
   - `Recommended Actions` 섹션 추가
   - `alias_candidate`, `error_alias_candidate`, `normalization_candidate`, `manual_review`로 분류
2. 반복 빈도가 높은 안전 케이스는 normalization rule로 승격
   - 공백/단위/OCR zero 용량 패턴을 normalization 후보로 분류
   - 실제 rule 승격은 Phase 20의 `5 O O mg -> 500mg` 회귀 테스트로 고정
3. 상품명/성분명만 존재하는 케이스는 관리자 alias 승인 흐름으로 연결
   - vector/fuzzy/ingredient 기반 후보는 자동 alias 대신 `manual_review` 유지
   - 안정적인 item_seq와 prefix/exact 계열 후보만 alias 후보로 export
4. 운영 리포트 결과를 관리자 화면 또는 주기 작업으로 노출할지 결정
   - 1차 구현은 JSON export 및 선택적 DB upsert로 결정
   - `--alias-candidates-output`으로 리뷰 파일 생성
   - `--upsert-alias-candidates`로 `medication_alias_suggestions`에 PENDING 후보 등록
   - 기존 APPROVED/REJECTED 후보는 review_status를 덮어쓰지 않고 frequency도 보존
5. `scripts/seed_aliases.py` 기존 들여쓰기 오류 정리 후 전체 scripts compileall 복구

검증:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m pytest tests/unit_tests/test_analyze_ocr_quality.py tests/unit_tests/test_ocr_quality_regressions.py
.\.venv\Scripts\python.exe -m compileall -f scripts
.\.venv\Scripts\python.exe -m scripts.analyze_ocr_quality --help
```

## Phase 22. 운영 리포트 실행과 관리자 UX 연결 - 로컬 구현 완료

우선순위: 중간  
예상 소요: 4~8시간

완료:

1. 운영/스테이징 DB에서 `analyze_ocr_quality.py`를 실제 실행해 `ocr_quality_report.md`와 `ocr_alias_candidates.json` 생성
   - 실행 명령과 검토 기준을 `docs/ocr_ops.md`에 정리
   - `docs/ocr_quality_report.md`에 alias 후보 export/upsert 명령 추가
2. 생성된 후보 중 normalization rule로 승격 가능한 패턴을 선별하고 회귀 테스트 추가
   - `normalization_candidate`는 운영 리포트에서 먼저 수집하고, rule 승격 시 `test_ocr_quality_regressions.py`에 추가하는 절차로 고정
3. `--upsert-alias-candidates`로 등록된 후보가 관리자 AliasManagement 화면에 정상 노출되는지 E2E 확인
   - `AliasManagement.tsx`에 `source_request_id` 표시 추가
   - `source=ocr_quality_report` 후보를 품질 리포트 배지로 구분
   - 빈도 `frequency`, 출처 `source`, 요청 ID를 함께 보며 승인/거절 가능
4. 리포트 실행을 수동 운영 명령으로 둘지, 배치/관리자 버튼으로 노출할지 결정
   - 1차는 수동 운영 명령으로 유지
   - DB write는 `--upsert-alias-candidates` 플래그를 명시할 때만 수행
5. 후보 승인 후 `reload-dictionary`와 vector status 확인까지 운영 절차로 묶기
   - `docs/ocr_ops.md`에 관리자 승인, dictionary reload, vector status 확인 절차 추가
   - `docs/chromadb_ops.md`에서 OCR 운영 runbook을 참조하도록 연결

검증:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m pytest tests/unit_tests/test_analyze_ocr_quality.py tests/unit_tests/test_ocr_quality_regressions.py tests/unit_tests/test_medication_pipeline.py tests/unit_tests/test_drug_dictionary_index.py tests/unit_tests/test_ocr_owner_endpoint.py
.\.venv\Scripts\python.exe -m compileall -f app/ocr app/api app/core scripts

cd ..\..\FE\SilverLink-FE
npm.cmd run build
```

운영 환경에서 추가 확인 필요:

1. 실제 DB 연결 정보로 `analyze_ocr_quality.py` 실행
2. 생성된 `docs/ocr_alias_candidates.json` 후보 수동 검토
3. `--upsert-alias-candidates` 실행 후 관리자 화면에서 `source=ocr_quality_report` 후보 노출 확인
4. 승인/거절 후 dictionary reload 및 vector status 확인

## Phase 23. 운영 실행 결과 기반 품질 개선 - 로컬 도구 구현 완료

우선순위: 중간  
예상 소요: 운영 데이터 규모에 따라 2~8시간

완료:

1. 실제 리포트의 `normalization_candidate`를 검토해 안전한 rule만 `TextNormalizer`에 추가
   - 운영 실행 전/후 metrics JSON 저장 옵션 추가
   - 실제 샘플 기반 rule 승격은 운영 리포트 생성 후 진행
2. 실제 리포트의 `manual_review` 케이스를 분석해 vector/fuzzy/ingredient 후보 UX 개선 여부 결정
   - 전후 비교에서 `recommended_action_delta`와 action type별 delta를 확인하도록 비교 리포트 추가
3. 승인된 alias/error_alias 반영 후 같은 OCR 샘플의 decision_status 개선 여부 측정
   - `--metrics-output`으로 before/after JSON 저장
   - `--compare-before`, `--compare-after`, `--comparison-output`으로 `docs/ocr_quality_comparison.md` 생성
   - 비교 항목: decision status, match method, recommended action type
4. 반복 리포트를 주기 작업으로 둘지, 관리자 수동 버튼으로 만들지 결정
   - 현 단계는 수동 운영 명령 유지
   - 운영 결과가 안정되면 Phase 24에서 배치/관리자 버튼 여부 결정
5. 운영 결과를 기준으로 confidence threshold와 자동 확정 기준 재조정
   - threshold 조정은 실제 before/after 비교 결과 확보 후 진행

검증:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m pytest tests/unit_tests/test_analyze_ocr_quality.py tests/unit_tests/test_ocr_quality_regressions.py tests/unit_tests/test_medication_pipeline.py tests/unit_tests/test_drug_dictionary_index.py tests/unit_tests/test_ocr_owner_endpoint.py
.\.venv\Scripts\python.exe -m compileall -f app/ocr app/api app/core scripts
.\.venv\Scripts\python.exe -m scripts.analyze_ocr_quality --help
```

운영 환경에서 추가 확인 필요:

1. 승인/upsert 전 `--metrics-output docs/ocr_quality_metrics_before.json` 생성
2. 후보 승인, dictionary reload, vector status 확인
3. 승인/upsert 후 `--metrics-output docs/ocr_quality_metrics_after.json` 생성
4. `--compare-before`, `--compare-after`로 `docs/ocr_quality_comparison.md` 생성
5. 비교 결과를 기준으로 normalization rule, threshold, UX 개선 여부 결정

## Phase 24. 운영 자동화 또는 관리자 실행 버튼 - 완료

우선순위: 낮음~중간  
예상 소요: 4~10시간

목표:

- 운영자가 CLI 없이 관리자 화면에서 OCR 품질 리포트를 실행하고 결과를 확인할 수 있게 한다.
- DB write가 필요한 alias 후보 upsert는 명시적인 관리자 액션으로 분리한다.
- before/after 비교 결과를 관리자 화면에서 요약 확인할 수 있게 한다.

권장 방향:

- 1차 구현은 "관리자 수동 버튼"으로 진행한다.
- 배치 자동화는 운영 데이터로 리포트 비용과 실행 시간을 확인한 뒤 Phase 25에서 판단한다.
- 리포트 생성은 read-only API로 시작하고, upsert는 별도 endpoint와 별도 버튼으로 분리한다.

완료:

### 24A. AI internal quality report endpoint 설계/구현

Endpoint:

- `POST /api/ocr/admin/quality-report/run`
- 인증: `X-SilverLink-Secret` 필수
- 기본 동작: read-only 리포트 생성
- request:
  - `limit`: 기본 20
  - `include_candidates`: 기본 true
  - `persist_files`: 기본 false
- response:
  - `success`
  - `generated_at`
  - `decision_counts`
  - `suggestion_counts`
  - `match_method_counts`
  - `recommended_action_counts`
  - `alias_candidate_count`
  - `manual_review_count`
  - `normalization_candidate_count`
  - `report_markdown`
  - `alias_candidates`

완료 기준:

- 기존 `scripts/analyze_ocr_quality.py`의 순수 함수 재사용 완료
- endpoint는 기본 read-only로 구현
- DB 연결 실패 시 HTTP 500과 명확한 message 반환
- AI unit test 추가

### 24B. AI internal alias candidate upsert endpoint 설계/구현

Endpoint:

- `POST /api/ocr/admin/quality-report/upsert-alias-candidates`
- 인증: `X-SilverLink-Secret` 필수
- request:
  - `limit`: 기본 20
  - `confirm_write`: 반드시 true
- response:
  - `success`
  - `upserted_count`
  - `candidate_count`
  - `skipped_count`
  - `message`

안전 조건:

- `confirm_write != true`이면 400 반환
- `alias_candidate`, `error_alias_candidate`만 upsert
- `manual_review`, `normalization_candidate`는 DB write 제외
- 기존 APPROVED/REJECTED 상태는 덮어쓰지 않음

완료 기준:

- 잘못된 confirm 요청 테스트 추가
- upsert payload 분리 테스트 추가
- 기존 `upsert_alias_candidates()` 재사용

### 24C. BE admin proxy 추가

Endpoint:

- `POST /api/admin/ocr/quality-report/run`
- `POST /api/admin/ocr/quality-report/upsert-alias-candidates`

보안:

- `@PreAuthorize("hasRole('ADMIN')")`
- BE -> AI 호출 시 `X-SilverLink-Secret` 전달
- AI 오류 status 보존

완료 기준:

- DTO 추가
- controller test 추가
- 기존 `AdminOcrController`에 최소 범위 추가

### 24D. FE 관리자 화면 연결

위치:

- 1차: `AliasManagement.tsx` 운영 패널에 버튼 추가
- 장기: 별도 OCR 운영 화면 분리

UI:

- `품질 리포트 실행` 버튼
- `alias 후보 등록` 버튼은 별도 confirm dialog 또는 명확한 경고 문구와 함께 제공
- 요약 카드:
  - decision status counts
  - alias 후보 수
  - manual review 수
  - normalization 후보 수
  - generated_at
- 실행 후 `source=ocr_quality_report` 후보가 목록에 보이도록 refresh

완료 기준:

- FE API client 추가
- loading/error/success 상태 처리
- `npm run build` 통과

### 24E. comparison report 노출 방식 결정

1차 범위:

- API는 latest run summary만 반환
- before/after 비교는 CLI 문서 유지

후속 범위:

- comparison JSON을 저장할 테이블 또는 파일 저장소가 생기면 관리자 화면에 비교 카드 추가

완료 기준:

- Phase 24에서는 comparison UI를 제외하고 read-only latest summary만 제공
- `ocr_ops.md`에 관리자 버튼과 CLI comparison 분리 이유 기록

### 24F. 감사/운영 로그

필수 로그:

- 실행자: BE에서 전달 가능한 경우 admin username
- 실행 시각
- limit
- generated action count
- upsert candidate count
- upsert 실행 여부

완료 기준:

- 최소 BE error log와 AI endpoint error handling 유지
- 별도 audit table은 Phase 25 후보로 분리

검증:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m pytest tests/unit_tests/test_analyze_ocr_quality.py tests/unit_tests/test_ocr_owner_endpoint.py
.\.venv\Scripts\python.exe -m compileall -f app/ocr app/api app/core scripts

cd BE\SilverLink-BE
powershell -ExecutionPolicy Bypass -File .\scripts\gradle-jdk21.ps1 test --tests "com.aicc.silverlink.domain.ocr.controller.*"

cd FE\SilverLink-FE
npm.cmd run build
```

결과:

- AI endpoint/report tests: 18 passed
- AI compileall: 성공
- BE `com.aicc.silverlink.domain.ocr.controller.*`: 성공
- FE `npm.cmd run build`: 성공
- FE build의 Browserslist/chunk size 경고는 기존 운영 경고로 유지

리스크:

- 품질 리포트 쿼리가 운영 DB에서 느릴 수 있다. 초기 limit은 작게 유지한다.
- 관리자 버튼으로 DB write를 열면 오등록 위험이 있으므로 upsert는 별도 버튼/confirm으로 분리한다.
- 운영 환경에서 AI 내부 endpoint가 외부 노출되지 않도록 BE proxy 경유만 사용한다.

## Phase 25. OCR operations audit/history hardening - completed

Priority: medium  
Actual scope: BE structured history + audit log, FE recent-run/trend panel, operations docs

Completed:

1. Added BE structured run history storage in `ocr_quality_report_runs` for quality report execution and alias upsert execution.
2. Reused the existing `AuditLogService` for compact admin audit metadata while keeping full report bodies out of audit payloads.
3. Added `GET /api/admin/ocr/quality-report/runs?limit=10` for recent execution history and latest-vs-previous trend deltas.
4. Updated the admin Alias Management OCR quality panel to load recent runs, refresh after report/upsert actions, and show trend cards plus the last five runs.
5. Updated `docs/ocr_ops.md` with the run-history endpoint and operations guidance.

Verification:

```powershell
cd BE\SilverLink-BE
powershell -ExecutionPolicy Bypass -File .\scripts\gradle-jdk21.ps1 test --tests "com.aicc.silverlink.domain.ocr.controller.*"

cd FE\SilverLink-FE
npm.cmd run build
```

Result:

- BE `com.aicc.silverlink.domain.ocr.controller.*`: success
- FE `npm.cmd run build`: success
- FE build still prints the existing Browserslist/chunk-size warnings.

## Phase 26. OCR operations automation and approval prioritization - next

Priority: medium  
Estimated effort: 6-10 hours

Recommended tasks:

1. Add a scheduled or manually triggered job policy for periodic quality report generation in operations environments.
2. Store comparison snapshots if the team wants long-term before/after trend charts beyond the latest two report runs.
3. Add prioritization fields for alias candidates, such as risk score, frequency score, or recommended review order.
4. Split the Alias Management OCR operations area into a dedicated admin OCR Operations page if the panel keeps growing.
5. Add service/repository tests around `OcrQualityReportRunService` trend calculations when persistence behavior becomes shared by automation.

---
## 5. 추천 실행 순서

1. Phase 26: OCR operations automation and approval prioritization

---

## 6. 명령 모음

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m pytest tests/unit_tests
.\.venv\Scripts\python.exe -m scripts.load_drug_data --chromadb-only --reset-chromadb
.\.venv\Scripts\python.exe -c "from app.ocr.repository.drug_vector_repository import DrugVectorRepository; print(DrugVectorRepository().get_count())"
```

```powershell
cd BE\SilverLink-BE
.\scripts\gradle-jdk21.ps1 test
```

```powershell
cd FE\SilverLink-FE
npm.cmd run build
```
