# AttendMark Windows Desktop Installer
$ErrorActionPreference = "Stop"

Write-Host "===============================================" -ForegroundColor Green
Write-Host "       AttendMark Windows Desktop Installer     " -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green
Write-Host ""

$InstallDir = "$env:LOCALAPPDATA\AttendMark"
$SourceDir = $PSScriptRoot

if (Test-Path $InstallDir) {
    Write-Host "[INFO] Previous installation found. Removing existing files..." -ForegroundColor Yellow
    # Close any running instances of the app to avoid file locking
    Stop-Process -Name "AttendMark" -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
    Remove-Item -Recurse -Force $InstallDir
}

Write-Host "Installing AttendMark to local app directory..." -ForegroundColor Cyan
New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null

# Copy everything from compiled distribution to installation directory
Copy-Item -Path "$SourceDir\*" -Destination $InstallDir -Recurse -Force | Out-Null

# Create shortcuts
$WshShell = New-Object -ComObject WScript.Shell

# Desktop Shortcut
$DesktopShortcutPath = "$env:USERPROFILE\Desktop\AttendMark.lnk"
Write-Host "Creating Desktop shortcut..." -ForegroundColor Cyan
$Shortcut = $WshShell.CreateShortcut($DesktopShortcutPath)
$Shortcut.TargetPath = "$InstallDir\AttendMark.exe"
$Shortcut.WorkingDirectory = $InstallDir
$Shortcut.Description = "AttendMark - Teacher Attendance & Marks Register"
$Shortcut.Save()

# Start Menu Shortcut
$StartMenuDir = "$env:APPDATA\Microsoft\Windows\Start Menu\Programs\AttendMark"
if (!(Test-Path $StartMenuDir)) {
    New-Item -ItemType Directory -Path $StartMenuDir -Force | Out-Null
}
$StartMenuShortcutPath = "$StartMenuDir\AttendMark.lnk"
Write-Host "Creating Start Menu shortcut..." -ForegroundColor Cyan
$Shortcut = $WshShell.CreateShortcut($StartMenuShortcutPath)
$Shortcut.TargetPath = "$InstallDir\AttendMark.exe"
$Shortcut.WorkingDirectory = $InstallDir
$Shortcut.Description = "AttendMark - Teacher Attendance & Marks Register"
$Shortcut.Save()

# Create native Uninstaller batch script
$UninstallScript = @"
@echo off
title Uninstall AttendMark
echo ==================================================
echo               Uninstall AttendMark
echo ==================================================
echo.
echo Closing running processes...
taskkill /f /im AttendMark.exe >nul 2>&1
echo Removing application files...
rmdir /s /q "$InstallDir"
echo Removing shortcuts...
del /f /q "%USERPROFILE%\Desktop\AttendMark.lnk" >nul 2>&1
del /f /q "%APPDATA%\Microsoft\Windows\Start Menu\Programs\AttendMark\AttendMark.lnk" >nul 2>&1
del /f /q "%APPDATA%\Microsoft\Windows\Start Menu\Programs\AttendMark\Uninstall AttendMark.lnk" >nul 2>&1
rmdir /s /q "%APPDATA%\Microsoft\Windows\Start Menu\Programs\AttendMark" >nul 2>&1
echo.
echo AttendMark has been successfully uninstalled from your PC.
echo.
pause
"@
$UninstallScript | Out-File -FilePath "$InstallDir\Uninstall.bat" -Encoding ascii

# Create Start Menu Uninstaller Shortcut
$UninstallShortcutPath = "$StartMenuDir\Uninstall AttendMark.lnk"
$Shortcut = $WshShell.CreateShortcut($UninstallShortcutPath)
$Shortcut.TargetPath = "$InstallDir\Uninstall.bat"
$Shortcut.WorkingDirectory = $InstallDir
$Shortcut.Description = "Uninstall AttendMark"
$Shortcut.Save()

Write-Host ""
Write-Host "===============================================" -ForegroundColor Green
Write-Host " AttendMark has been successfully installed!" -ForegroundColor Green
Write-Host " You can launch it from your Desktop or Start Menu." -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green
Write-Host ""
Write-Host "Press any key to close..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
