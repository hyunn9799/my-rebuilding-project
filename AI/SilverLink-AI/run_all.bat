@echo off
echo ========================================
echo   SilverLink Full System Launcher
echo   (API Server + SQS Worker)
echo ========================================
echo.

REM Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python is not installed or not in PATH
    echo Please install Python 3.12 or higher
    pause
    exit /b 1
)

echo [INFO] Starting both API Server and SQS Worker...
echo [INFO] API Server: http://localhost:5000
echo [INFO] API Docs: http://localhost:5000/docs
echo.

REM Create logs directory if not exists
if not exist "logs" mkdir logs

REM Start API Server in background
echo [1/2] Starting API Server...
start "SilverLink API" cmd /k "python -m uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload"

REM Wait a bit for API to start
timeout /t 3 /nobreak >nul

REM Start Worker in background
echo [2/2] Starting SQS Worker...
start "SilverLink Worker" cmd /k "python worker_main.py"

echo.
echo ========================================
echo   Both services are starting...
echo   Check the opened windows for logs
echo ========================================
echo.
echo Press any key to exit this launcher
echo (Services will continue running)
pause >nul
