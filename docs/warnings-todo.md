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
  - 권장 작업: 운영 profile에서 실행되지 않도록 profile 조건 또는 dev/test 전용 initializer로 분리.

## AI OCR

- `test_db_connection.py`에서 `medication_ocr_candidates` 테이블이 누락된 것으로 표시됨.
  - 권장 작업: 현재 코드가 후보 상세 테이블을 실제로 사용하는지 확인.
  - 권장 작업: 사용한다면 `schema.sql` 또는 AI repository 초기화 경로와 실제 MySQL 스키마를 맞춤.
- `AI/SilverLink-AI/chroma_db/chroma.sqlite3`가 런타임 중 수정됨.
  - 권장 작업: ChromaDB를 재생성 가능한 산출물로 볼지, 버전 관리 대상에서 제외할지 결정.
- AI 서비스 시작 시 선택 의존성 누락 안내가 출력됨.
  - 확인된 메시지: `qdrant_client not found`, `llama-cpp-python not found`, `Presidio library not found`, `Mem0 library not found`.
  - 권장 작업: 실제 운영 기능에 필요한 의존성과 optional 기능을 구분하고 README/env 문서에 명시.
- Python 실행은 WindowsApps alias가 아니라 `AI/SilverLink-AI/.venv/Scripts/python.exe`를 사용해야 함.
  - 권장 작업: 로컬 실행 문서와 스크립트에 venv python 경로를 명시.

## OCR Integration

- 관리자 Alias UI의 실제 브라우저 검증은 API 레벨까지 완료했고, Playwright 기반 UI 조작 검증은 미실행.
  - 권장 작업: FE dev server + BE + AI를 띄운 뒤 `/admin/alias-management`에서 목록/승인/거부 버튼 E2E 테스트 추가.
- `reload-dictionary`는 BE 경유로 성공 확인했지만, 대량 alias 승인 직후 성능/락 영향은 미검증.
  - 권장 작업: LocalDrugIndex reload 시간과 요청 중 동시성 영향을 측정.
