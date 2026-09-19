@echo off
title AttendMark Desktop
echo ==================================================
echo       AttendMark Desktop Client (Windows)
echo ==================================================
echo.
echo Checking Node.js environment...
node -v >nul 2>&1
if %errorlevel% neq 0 (
    echo [INFO] Node.js is not installed.
    echo Launching AttendMark as a native local HTML app...
    echo.
    start "" "index.html"
    exit /b
)

if not exist node_modules (
    echo.
    echo Installing required desktop components (Electron)...
    echo This may take a moment on first startup...
    echo.
    call npm install
)

echo.
echo Launching AttendMark Desktop...
echo.
npx electron .
