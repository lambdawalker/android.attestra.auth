@echo off
setlocal
where uv >nul 2>nul
if errorlevel 1 (
    echo uv is required. Install it from https://docs.astral.sh/uv/getting-started/installation/ 1>&2
    exit /b 127
)
uv run "%~dp0scripts\signing\signing.py" %*
exit /b %errorlevel%
