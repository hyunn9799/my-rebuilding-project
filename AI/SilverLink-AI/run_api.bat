@echo off
echo ========================================
echo   SilverLink FastAPI Server Launcher
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

echo [INFO] Starting SilverLink FastAPI Server...
echo [INFO] Server will be available at http://localhost:5000
echo [INFO] API Documentation: http://localhost:5000/docs
echo [INFO] Press Ctrl+C to stop the server
echo.

REM Create logs directory if not exists
if not exist "logs" mkdir logs

REM Run the API server
python -m uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload

REM If server exits, show message
echo.
echo ========================================
echo   Server has stopped
echo ========================================
pause
