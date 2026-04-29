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

### Phase 16B. BE admin vector-status proxy

우선순위: 중간  
예상 소요: 1~2시간

목표:

- FE가 AI internal endpoint를 직접 호출하지 않도록 BE admin endpoint로만 vector 상태를 노출한다.

작업:

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

## Phase 17. OCR 후보 테이블 정합성 점검

우선순위: 중간  
예상 소요: 1~2시간

작업:

1. `medication_ocr_candidates` 참조 여부 검색
2. 사용하지 않으면 문서/테스트에서 제거
3. 사용한다면 schema/repository 초기화 경로 추가
4. OCR result JSON 저장 방식과 후보 테이블 분리 방식 중 하나로 확정

## Phase 18. 사용자 복약 등록까지 E2E

우선순위: 중간  
예상 소요: 3~5시간

작업:

1. Senior OCR 화면에서 OCR 실행
2. 후보 확인 모달에서 선택
3. `confirmMedication` 호출 확인
4. 복약 일정 등록 다이얼로그 필드 매핑 확인
5. medication schedule 저장 API 확인
6. dashboard 미확인 badge 갱신 확인

## Phase 19. 성능/운영 안정화

우선순위: 중간  
예상 소요: 2~4시간

작업:

1. `reload-dictionary` 실행 시간 측정
2. reload 중 atomic swap 동작 확인
3. ChromaDB 검색 latency 측정
4. optional dependency 경고 정리
5. Windows 로컬 실행 문서에 `.venv\Scripts\python.exe` 사용 명시

## Phase 20. 데이터 품질 개선

우선순위: 낮음~중간  
예상 소요: 2~6시간

작업:

1. e약은요 효능/복용법 데이터 병합 여부 결정
2. 승인된 alias/error alias 분석
3. 반복 OCR 오류를 normalization rule 또는 error alias로 승격
4. 성분명 기반 후보 UX 개선

---

## 5. 추천 실행 순서

1. Phase 16B: BE admin vector-status proxy
2. Phase 17: OCR 후보 테이블 정합성 점검
3. Phase 18: 사용자 복약 등록 E2E
4. Phase 19: 성능/운영 안정화
5. Phase 20: 데이터 품질 개선

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
