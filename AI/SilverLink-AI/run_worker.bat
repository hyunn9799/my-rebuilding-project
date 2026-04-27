@echo off
echo ========================================
echo   SilverLink SQS Worker Launcher
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

echo [INFO] Starting SilverLink SQS Worker...
echo [INFO] Press Ctrl+C to stop the worker
echo.

REM Create logs directory if not exists
if not exist "logs" mkdir logs

REM Run the worker
python worker_main.py

REM If worker exits, show message
echo.
echo ========================================
echo   Worker has stopped
echo ========================================
pause
