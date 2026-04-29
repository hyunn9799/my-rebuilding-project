# Warnings TODO

2026-04-29 기준으로 빌드/검증 중 확인했지만 이번 작업 범위에서는 보류한 항목입니다.

## Frontend

- `npm.cmd run build` 경고: Browserslist `caniuse-lite` 데이터가 오래됨.
  - 권장 작업: `npx update-browserslist-db@latest` 실행 후 lockfile 변경 확인.
- `npm.cmd run build` 경고: 일부 Vite chunk가 500KB를 초과함.
  - 권장 작업: 라우트 단위 lazy loading 상태 재점검.
  - 권장 작업: `vite.config.ts`의 `build.rollupOptions.output.manualChunks` 적용 검토.
  - 권장 작업: 큰 공통 번들 원인 분석. 특히 `index-*.js`, 폰트, 지도/차트/이미지 압축 관련 의존성 확인.

## Backend

- 기본 `application.yml`로 `bootRun` 실행 시 AWS S3 환경변수 placeholder가 없으면 부팅 실패.
  - 권장 작업: 로컬 개발 profile 분리 또는 `cloud.aws.s3.enabled=false`를 명확히 적용하는 dev profile 추가.
- `application-test.yml`은 H2를 사용하므로 실제 MySQL/Redis 연결 상태 검증과 분리되어 있음.
  - 권장 작업: 로컬 통합 검증용 profile을 별도로 만들고, 관리자 seed 계정 포함 여부를 명시.
- 테스트용 관리자 계정 `admin_test / 1234`가 `DataInitializer`에 추가됨.
  - 상태: `DataInitializer`에 `@Profile("!prod")`를 적용해 운영 profile 실행은 차단함.
  - 남은 작업: dev/test 전용 initializer로 파일을 분리할지 결정.
- Gradle 테스트 중 deprecated API 경고가 출력됨.
  - 확인된 파일: `BE/SilverLink-BE/src/main/java/com/aicc/silverlink/domain/ocr/controller/OcrProxyController.java`
  - 확인된 파일: `BE/SilverLink-BE/src/test/java/com/aicc/silverlink/domain/ocr/controller/OcrProxyControllerTest.java`
  - 권장 작업: Spring 4/7 계열에서 deprecated 처리된 API 사용 지점을 확인하고 대체 API로 교체.

## Docker / Compose

- `docker compose config` 실행 시 일부 환경변수 미설정 경고가 출력됨.
  - 확인된 변수: `SILVERLINK_NUMBER`, `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `SQS_QUEUE_URL`, `SQS_DLQ_URL`.
  - 권장 작업: 로컬용 `.env.example` 또는 compose override에서 빈 값 허용 여부를 명확히 문서화.
  - 권장 작업: 실제 운영 필수 변수와 선택 변수를 분리.
- `docker-compose.yml`의 obsolete `version` 경고가 있었음.
  - 상태: `version` 키 제거로 해결.

## AI OCR

- `test_db_connection.py`에서 `medication_ocr_candidates` 테이블이 누락된 것으로 표시됨.
  - 권장 작업: 현재 코드가 후보 상세 테이블을 실제로 사용하는지 확인.
  - 권장 작업: 사용한다면 `schema.sql` 또는 AI repository 초기화 경로와 실제 MySQL 스키마를 맞춤.
- `AI/SilverLink-AI/chroma_db/chroma.sqlite3`가 런타임 중 수정됨.
  - 상태: ChromaDB는 Git 제외 생성 데이터로 결정했고, 운영 기본 방식은 persistent volume으로 문서화함.
  - 참고 문서: `AI/SilverLink-AI/docs/chromadb_ops.md`
  - 남은 작업: vector status endpoint와 startup warning 구현.
- `AI/SilverLink-AI/mem_db/meta.json`이 아직 Git 추적 중임.
  - 권장 작업: `mem_db`를 생성 데이터로 볼지 확인하고, 필요하면 Git 추적 제거 및 `.gitignore` 정리.
- AI 서비스 시작 시 선택 의존성 누락 안내가 출력됨.
  - 확인된 메시지: `qdrant_client not found`, `llama-cpp-python not found`, `Presidio library not found`, `Mem0 library not found`.
  - 권장 작업: 실제 운영 기능에 필요한 의존성과 optional 기능을 구분하고 README/env 문서에 명시.
- Python 실행은 WindowsApps alias가 아니라 `AI/SilverLink-AI/.venv/Scripts/python.exe`를 사용해야 함.
  - 권장 작업: 로컬 실행 문서와 스크립트에 venv python 경로를 명시.
- AI unit test 실행 시 pytest warnings count가 출력됨.
  - 확인된 출력: 전체 unit `5 warnings`, `test_vector_status.py` + `test_ocr_owner_endpoint.py` `4 warnings`.
  - 현재 `-W always -ra`로도 상세 warning message가 출력되지 않음.
  - 권장 작업: `pytest.ini` warning filter 또는 plugin 출력 설정을 확인해 warning 상세가 보이도록 조정.

## OCR Integration

- 관리자 Alias UI의 실제 브라우저 검증은 API 레벨까지 완료했고, Playwright 기반 UI 조작 검증은 미실행.
  - 권장 작업: FE dev server + BE + AI를 띄운 뒤 `/admin/alias-management`에서 목록/승인/거부 버튼 E2E 테스트 추가.
- `reload-dictionary`는 BE 경유로 성공 확인했지만, 대량 alias 승인 직후 성능/락 영향은 미검증.
  - 권장 작업: LocalDrugIndex reload 시간과 요청 중 동시성 영향을 측정.
