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
| 사용자 확인 API | 완료 | `validate-medication`, `confirm-medication`, `pending-confirmations` |
| Alias suggestion | 완료 | 사용자 확정 시 `medication_alias_suggestions`에 `PENDING` 생성 확인 |
| 관리자 Alias API | 완료 | AI/BE 목록, 승인, 거부, dictionary reload 구현 및 API E2E 확인 |
| 관리자 Alias FE | 완료 | `/admin/alias-management` 화면, 필터, 승인/거부, reload 버튼 구현 |
| ChromaDB | 완료 | 활성 의약품 35,291건 전체 적재, 컬렉션 `drug_embeddings` |
| 데이터 파이프라인 | 완료 | API 적재와 `--chromadb-only` 임베딩 재생성 분리 |
| Backend 빌드 | 완료 | JDK 21 기준 Gradle 빌드/테스트 성공 이력, `admin_test / 1234` 테스트 계정 추가 |
| Frontend 빌드 | 완료 | `npm.cmd run build` 성공, 경고는 `docs/warnings-todo.md`로 분리 |
| 문서/하네스 | 진행 중 | `AGENTS.md`, `.agents/skills/*`, `docs/architecture-map.md`, `docs/warnings-todo.md` |

---

## 2. 검증 완료 내역

### AI

- `tests/unit_tests`: `37 passed, 2 skipped`
- `app/api/endpoints/ocr.py`, `drug_dictionary_index.py`, `scripts/load_drug_data.py` compile 확인 대상
- 실제 MySQL 연결 확인
- Alias suggestion 관리자 API 직접 검증
- 사용자 confirm 흐름 검증
- ChromaDB 전체 적재 후 count `35,291` 확인
- LLM descriptor에서 공식 효능 정보가 없을 때 효능을 유추하지 않도록 fallback 문구 적용

### Backend

- `BE/SilverLink-BE/scripts/gradle-jdk21.ps1` 추가
- `SessionServiceTest`를 현재 Redis pipeline/HMGET/Lua varargs 동작에 맞게 수정
- `AdminAliasControllerTest` 추가
- AI/BE 서버 기동 후 관리자 토큰 기반 alias 목록/승인/거부/reload E2E 확인

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

## Phase 12. Vector fallback 테스트 보강

우선순위: 높음  
예상 소요: 2~4시간

작업:

1. `MedicationPipeline`에서 MySQL/LocalDrugIndex 저신뢰 또는 실패 시 `VectorMatcher` 호출 조건 확인
2. 벡터 후보의 score/distance가 confidence에 반영되는 방식 정리
3. 단위 테스트 추가
   - 상품명 일부 OCR
   - 성분명만 OCR
   - 제조사 포함 OCR
   - alias에는 없지만 vector로 가까운 후보가 있는 경우
   - 무관한 OCR 텍스트에서 낮은 confidence 유지
4. vector-only 후보는 자동 확정되지 않고 사용자 확인으로 가는지 검증

검증:

```powershell
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m pytest tests/unit_tests/test_medication_pipeline.py
.\.venv\Scripts\python.exe -m pytest tests/unit_tests
```

## Phase 13. 관리자 Alias 브라우저 E2E

우선순위: 높음  
예상 소요: 2~3시간

작업:

1. AI, BE, FE dev server 기동
2. `admin_test / 1234` 로그인
3. `/admin/alias-management` 접근
4. 테스트용 `medication_alias_suggestions` 삽입
5. 목록 노출, 승인, 거부, dictionary reload 버튼 검증
6. DB 반영 확인
7. Playwright smoke 또는 수동 검증 절차 문서화

## Phase 14. 보안/권한 정리

우선순위: 높음  
예상 소요: 2~4시간

작업:

1. `admin_test / 1234` seed를 dev/test profile 전용으로 제한
2. `/api/admin/alias-suggestions/**` ADMIN 권한 확인
3. `/api/ocr/confirm-medication` 본인/위임 권한 규칙 정의
4. `/api/ocr/pending-confirmations/{elderlyUserId}` 접근 권한 확인
5. AI 관리자 endpoint 운영 접근 경로를 BE 내부 프록시로 제한

## Phase 15. BE OCR 프록시 보강

우선순위: 중간  
예상 소요: 2~4시간

목표:

- FE가 AI 서버를 직접 호출하지 않고 Spring Boot BE를 통해 OCR API를 호출하도록 정리
- `/api/ocr/validate-medication`, `/api/ocr/confirm-medication`, `/api/ocr/pending-confirmations`의 인증/인가 책임을 BE에 둠
- `/api/ocr/admin/reload-dictionary`는 ADMIN 전용으로 제한

## Phase 16. ChromaDB 운영 정책 결정

우선순위: 중간  
예상 소요: 1~2시간

권장:

- `chroma_db/`는 Git 추적 제외
- 재생성 명령을 문서화
- 운영은 persistent volume 또는 별도 artifact로 관리
- 컬렉션 schema/version 메타데이터 추가 검토

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

1. Phase 12: Vector fallback 테스트 보강
2. Phase 13: 관리자 Alias 브라우저 E2E
3. Phase 14: 보안/권한 정리
4. Phase 15: BE OCR 프록시 보강
5. Phase 16: ChromaDB 운영 정책 결정
6. Phase 17: OCR 후보 테이블 정합성 점검
7. Phase 18: 사용자 복약 등록 E2E
8. Phase 19: 성능/운영 안정화
9. Phase 20: 데이터 품질 개선

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
