# AttendMark Single Setup Installer NSIS Script
Unicode true
SetCompressor /SOLID lzma

# Name and Output file
Name "AttendMark"
OutFile "AttendMarkSetup.exe"

# Default Installation Directory
InstallDir "$LOCALAPPDATA\AttendMark"

# Registry key to check for directory (if any)
InstallDirRegKey HKCU "Software\AttendMark" "Install_Dir"

# Request Application Privileges for user (No Admin required!)
RequestExecutionLevel user

# Page declarations
Page directory
Page instfiles

UninstPage uninstConfirm
UninstPage instfiles

# Installer Sections
Section "AttendMark Application" SecApp
  SetOutPath "$INSTDIR"
  
  # Include all compiled files from Electron unpack directory
  File /r "dist/win-unpacked/*"
  
  # Store installation folder in registry
  WriteRegStr HKCU "Software\AttendMark" "Install_Dir" "$INSTDIR"
  
  # Write uninstaller details to registry for Add/Remove Programs
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\AttendMark" "DisplayName" "AttendMark"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\AttendMark" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\AttendMark" "DisplayVersion" "1.0.0"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\AttendMark" "Publisher" "AttendMark"
  
  # Create Uninstaller executable
  WriteUninstaller "$INSTDIR\uninstall.exe"
  
  # Create Desktop Shortcut
  CreateShortcut "$DESKTOP\AttendMark.lnk" "$INSTDIR\AttendMark.exe" "" "$INSTDIR\AttendMark.exe" 0
  
  # Create Start Menu Shortcuts
  CreateDirectory "$SMPROGRAMS\AttendMark"
  CreateShortcut "$SMPROGRAMS\AttendMark\AttendMark.lnk" "$INSTDIR\AttendMark.exe" "" "$INSTDIR\AttendMark.exe" 0
  CreateShortcut "$SMPROGRAMS\AttendMark\Uninstall AttendMark.lnk" "$INSTDIR\uninstall.exe" "" "$INSTDIR\uninstall.exe" 0
SectionEnd

# Uninstaller Section
Section "Uninstall"
  # Stop any running process of AttendMark safely
  # (Will be managed by the installer / uninstaller runtime automatically or closed gracefully)
  
  # Delete shortcuts
  Delete "$DESKTOP\AttendMark.lnk"
  Delete "$SMPROGRAMS\AttendMark\AttendMark.lnk"
  Delete "$SMPROGRAMS\AttendMark\Uninstall AttendMark.lnk"
  RMDir "$SMPROGRAMS\AttendMark"
  
  # Delete Files
  Delete "$INSTDIR\uninstall.exe"
  RMDir /r "$INSTDIR"
  
  # Delete Registry keys
  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\AttendMark"
  DeleteRegKey HKCU "Software\AttendMark"
SectionEnd
