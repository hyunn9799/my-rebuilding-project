@echo off
setlocal
echo ==============================================
echo [Phase 10] SilverLink-AI Medication Data Pipeline
echo ==============================================

set PYTHONPATH=%CD%

echo [1] MySQL 데이터 적재 (의약품 허가정보 API 연동)
python -m scripts.load_drug_data
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] MySQL DB 적재 실패!
    exit /b %ERRORLEVEL%
)

echo [2] Alias Seed 적재 (수동 실행 방어 적용)
python -m scripts.seed_aliases --force
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Alias Seed 적재 실패!
    exit /b %ERRORLEVEL%
)

echo [3] ChromaDB 임베딩 적재 (활성 약품 기준 백그라운드 벡터화)
python -m scripts.load_drug_data --chromadb-only
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] ChromaDB 벡터 적재 실패!
    exit /b %ERRORLEVEL%
)

echo ==============================================
echo 데이터 파이프라인 적재가 성공적으로 완료되었습니다.
echo ==============================================
endlocal
