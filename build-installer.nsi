; AttendMark Full Windows Installer NSIS Script
!define APPNAME "AttendMark"
!define COMPANY "AttendMark"
!define DESCRIPTION "AttendMark - Attendance & Grade Tracker"
!define VERSION "1.0.0"

Name "${APPNAME}"
OutFile "AttendMarkSetup.exe"
InstallDir "$LOCALAPPDATA\Programs\AttendMark"
Icon "AttendMark.ico"
UninstallIcon "AttendMark.ico"
RequestExecutionLevel user
SetCompressor /SOLID lzma

Page directory
Page instfiles

UninstPage uninstConfirm
UninstPage instfiles

Section "Install"
    SetOutPath "$INSTDIR"
    File "web/index.html"
    File "web/app.js"
    File "AttendMark.ico"
    File "AttendMark.exe"
    
    ; Create Start Menu and Desktop Shortcuts
    CreateDirectory "$SMPROGRAMS\AttendMark"
    CreateShortcut "$SMPROGRAMS\AttendMark\AttendMark.lnk" "$INSTDIR\AttendMark.exe" "" "$INSTDIR\AttendMark.ico" 0
    CreateShortcut "$SMPROGRAMS\AttendMark\Uninstall AttendMark.lnk" "$INSTDIR\Uninstall.exe" "" "$INSTDIR\AttendMark.ico" 0
    CreateShortcut "$DESKTOP\AttendMark.lnk" "$INSTDIR\AttendMark.exe" "" "$INSTDIR\AttendMark.ico" 0
    
    ; Write Uninstaller
    WriteUninstaller "$INSTDIR\Uninstall.exe"
    
    ; Add to Add/Remove Programs
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\AttendMark" "DisplayName" "AttendMark"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\AttendMark" "UninstallString" '"$INSTDIR\Uninstall.exe"'
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\AttendMark" "DisplayIcon" "$INSTDIR\AttendMark.ico"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\AttendMark" "Publisher" "AttendMark"
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\AttendMark" "DisplayVersion" "${VERSION}"

    ; Launch app after install
    Exec '"$INSTDIR\AttendMark.exe"'
SectionEnd

Section "Uninstall"
    Delete "$DESKTOP\AttendMark.lnk"
    Delete "$SMPROGRAMS\AttendMark\AttendMark.lnk"
    Delete "$SMPROGRAMS\AttendMark\Uninstall AttendMark.lnk"
    RMDir "$SMPROGRAMS\AttendMark"
    
    Delete "$INSTDIR\index.html"
    Delete "$INSTDIR\app.js"
    Delete "$INSTDIR\AttendMark.ico"
    Delete "$INSTDIR\AttendMark.exe"
    Delete "$INSTDIR\Uninstall.exe"
    RMDir "$INSTDIR"
    
    DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\AttendMark"
SectionEnd
