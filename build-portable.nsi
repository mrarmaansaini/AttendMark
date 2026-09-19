; AttendMark Portable Executable NSIS Script
!define APPNAME "AttendMark"
!define DESCRIPTION "AttendMark - Teacher Attendance & Marks Register"
!define VERSION "1.0.0"

Name "${APPNAME}"
OutFile "AttendMark.exe"
Icon "AttendMark.ico"
SilentInstall silent
RequestExecutionLevel user
SetCompressor /SOLID lzma

Section "Main"
    SetOutPath "$LOCALAPPDATA\AttendMark\app"
    File "web/index.html"
    File "web/app.js"
    File "web/tailwind.min.css"
    File "web/tailwind.cdn.js"
    File "web/lucide.min.js"
    File "AttendMark.ico"
    
    ; Check and launch via Edge or Chrome in app mode
    ReadRegStr $0 HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\msedge.exe" ""
    StrCmp $0 "" try_chrome launch_edge

try_chrome:
    ReadRegStr $0 HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\chrome.exe" ""
    StrCmp $0 "" launch_default launch_chrome

launch_edge:
    Exec '"$0" --app="file:///$LOCALAPPDATA\AttendMark\app\index.html" --user-data-dir="$LOCALAPPDATA\AttendMark\data" --allow-file-access-from-files --window-size=1360,900'
    Goto done

launch_chrome:
    Exec '"$0" --app="file:///$LOCALAPPDATA\AttendMark\app\index.html" --user-data-dir="$LOCALAPPDATA\AttendMark\data" --allow-file-access-from-files --window-size=1360,900'
    Goto done

launch_default:
    ExecShell "open" "$LOCALAPPDATA\AttendMark\app\index.html"

done:
SectionEnd
