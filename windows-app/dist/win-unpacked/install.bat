@echo off
title AttendMark Installer
echo ==================================================
echo       AttendMark Desktop Installer (Windows)
echo ==================================================
echo.
echo Installing AttendMark safely on your computer...
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0install.ps1"
