Phase 16A: Vector status endpoint 및 startup warning 구현 지시입니다.

목표:
ChromaDB persistent volume이 비어 있거나 잘못 마운트된 상태를 빠르게 감지할 수 있게 합니다.
ChromaDB는 약품 최종 확정 엔진이 아니라 VectorDB fallback 용도이므로, 문제가 있어도 서버 시작을 차단하지 않고 warning만 남깁니다.

구현 범위:
1. AI config 추가
2. VectorStatusResponse schema 추가
3. DrugVectorRepository.get_status(expected_count) 추가
4. 내부 endpoint 추가
5. startup warning 추가
6. unit test 추가

중요 정책:
- ChromaDB count가 0이거나 expected_count와 달라도 서버 시작을 막지 마세요.
- EMPTY, COUNT_MISMATCH, ERROR는 warning으로만 처리하세요.
- ChromaDB는 fallback이므로 상태 이상은 “서비스 중단”이 아니라 “검색 품질 저하 가능성”으로 봅니다.
- FE가 이 endpoint를 직접 호출하면 안 됩니다.
- 이 endpoint는 BE proxy 전용 internal endpoint입니다.
- X-SilverLink-Secret 헤더를 필수로 검증하세요.
- secret 값은 config/env에서 읽고, 코드에 하드코딩하지 마세요.
- secret 값은 로그에 남기지 마세요.

수정 대상 예상 파일:
- app/core/config.py
- app/ocr/schema/medication_schema.py 또는 status schema 파일
- app/ocr/repository/drug_vector_repository.py
- app/api/endpoints/ocr.py
- app/main.py 또는 FastAPI lifespan/startup 설정 파일
- tests/unit_tests 관련 파일
- docs/chromadb_ops.md

1. config 추가

다음 환경변수를 추가하세요.

DRUG_VECTOR_EXPECTED_COUNT=35291
DRUG_VECTOR_STARTUP_CHECK=true
SILVERLINK_INTERNAL_SECRET=<secret>

주의:
- DRUG_VECTOR_EXPECTED_COUNT는 현재 MySQL active 약품 수 기준값입니다.
- 약품 데이터 재적재 후 active_count가 바뀌면 이 값도 갱신해야 합니다.
- 값이 없거나 0이면 expected_count 비교를 생략할 수 있게 처리하세요.

2. VectorStatusResponse schema 추가

필드:
- collection_name: str
- persist_directory: str
- count: int | None
- expected_count: int | None
- embedding_model: str | None
- status: str
- message: str
- is_degraded: bool
- checked_at: datetime

status 값:
- READY
- EMPTY
- COUNT_MISMATCH
- ERROR

상태 규칙:
- READY: count > 0 이고 expected_count가 없거나 count == expected_count
- EMPTY: count == 0
- COUNT_MISMATCH: expected_count가 있고 count != expected_count
- ERROR: ChromaDB 초기화 또는 count 조회 실패

3. DrugVectorRepository.get_status(expected_count) 구현

기능:
- 현재 collection name 조회
- persist directory 조회
- collection count 조회
- embedding model 조회 가능하면 포함
- expected_count와 실제 count 비교
- 예외 발생 시 status=ERROR 반환
- 예외를 바깥으로 던져 서버 시작을 막지 마세요.

응답 예시:
{
  "collection_name": "drug_embeddings_permit_v1",
  "persist_directory": "chroma_db",
  "count": 35291,
  "expected_count": 35291,
  "embedding_model": "text-embedding-3-small",
  "status": "READY",
  "is_degraded": false,
  "message": "Vector store is ready.",
  "checked_at": "2026-04-29T10:30:00"
}

4. internal endpoint 추가

FastAPI 내부 endpoint:
GET /api/ocr/internal/vector/status

요구사항:
- X-SilverLink-Secret 헤더 필수
- 헤더 누락 또는 불일치 시 401 또는 403 반환
- FE 직접 호출 금지
- BE proxy 전용 endpoint라는 주석 추가
- status 조회 중 ERROR가 나도 HTTP 200으로 status=ERROR를 반환할지, HTTP 500으로 반환할지는 기존 프로젝트 규칙에 맞추세요.
  추천은 운영 상태 확인 목적상 HTTP 200 + status=ERROR입니다.
- 인증 실패는 반드시 401/403입니다.

5. startup warning 추가

FastAPI startup hook 또는 lifespan에서 서버 시작 시 1회 vector status를 확인하세요.

조건:
- DRUG_VECTOR_STARTUP_CHECK=true일 때만 실행
- READY면 info log
- EMPTY, COUNT_MISMATCH, ERROR면 warning log
- 어떤 경우에도 서버 시작을 차단하지 않음
- startup check 실패도 warning만 남김

로그 예시:
WARNING: ChromaDB status=EMPTY, count=0, expected_count=35291. Vector fallback will be degraded.
WARNING: ChromaDB status=COUNT_MISMATCH, count=1000, expected_count=35291. Run chromadb regeneration.
WARNING: ChromaDB status=ERROR. Vector fallback will be unavailable.

6. 테스트 추가

필수 테스트:
- READY: count > 0, expected_count 일치
- EMPTY: count == 0
- COUNT_MISMATCH: count와 expected_count 불일치
- ERROR: repository 조회 중 예외 발생
- secret 누락 시 401
- secret 불일치 시 401 또는 403
- secret 정상 시 status response 반환

검증 명령:
cd AI\SilverLink-AI
.\.venv\Scripts\python.exe -m pytest tests/unit_tests -q
.\.venv\Scripts\python.exe -m compileall -f app/ocr app/api app/core

7. 문서 업데이트

AI/SilverLink-AI/docs/chromadb_ops.md가 있다면 아래 내용을 추가하세요.

- Vector status endpoint 설명
- X-SilverLink-Secret은 BE proxy 전용이라는 점
- expected_count는 MySQL active_count 기준이라는 점
- count mismatch 시 조치 방법
- ChromaDB가 비어 있어도 서버는 시작되지만 Vector fallback 품질이 저하된다는 점
- 재생성 명령 예시

예시 명령:
python -m scripts.load_drug_data --chromadb-only --active-only --batch-size 100 --resume

이번 작업에서 하지 말 것:
- BE proxy 구현은 이번 Phase 16A에 포함하지 마세요.
- React 관리자 UI 구현하지 마세요.
- ChromaDB 전체 재생성 로직을 실행하지 마세요.
- Redis pub/sub 또는 dictionary_version polling 구현하지 마세요.
- 서버 시작을 ChromaDB 상태 때문에 실패시키지 마세요.
- MySQL/LocalDrugIndex 로직을 재작성하지 마세요.

완료 후 보고할 내용:
1. 수정/추가한 파일 목록
2. config 추가 내용
3. VectorStatusResponse schema
4. get_status 상태 판정 방식
5. internal endpoint 경로와 secret 검증 방식
6. startup warning 동작 방식
7. 테스트 결과
8. chromadb_ops.md 업데이트 여부
9. 남은 TODO