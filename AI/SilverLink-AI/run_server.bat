@echo off
cd /d "%~dp0"
echo Starting SilverLink AI Server...
set PYTHONPATH=src
uvicorn app.main:app --reload
pause
