;
; $Id: installer-common.nsi 20236 2005-04-01 23:17:56Z mdb $
;
; Installer include for use with all flavors of the installer

  ; The directory from which we will find our files at compile time
  !define BASEDIR ".."
  !define RSRCDIR "${BASEDIR}\${LOCALE}"
  !define DATADIR "${BASEDIR}\data"

  !define MUI_FILE "savefile"
  !define INSTALLER_VERSION "1.0"

  CRCCheck On
  OutFile ${OUTFILENAME}

  RequestExecutionLevel user

;--------------------------------
;General

  !define MUI_ICON "${RSRCDIR}\install_icon.ico"
  !define MUI_UNICON "${RSRCDIR}\uninstall_icon.ico"
  !define MUI_WELCOMEFINISHPAGE_BITMAP "${RSRCDIR}\branding.bmp"
  !define MUI_UNWELCOMEFINISHPAGE_BITMAP "${RSRCDIR}\branding.bmp"

  !include "MUI.nsh"
  !include "ZipDLL.nsh"

;--------------------------------
;Modern UI Configuration

  !ifndef NO_QUESTIONS_ASKED
    !insertmacro MUI_PAGE_WELCOME
    !ifdef REQUIRE_LICENSE
      !insertmacro MUI_PAGE_LICENSE "${RSRCDIR}\license.txt"
    !endif
    !insertmacro MUI_PAGE_DIRECTORY
  !endif
  !insertmacro MUI_PAGE_INSTFILES
  !ifndef NO_QUESTIONS_ASKED
    !insertmacro MUI_PAGE_FINISH
  !endif

  !ifndef NO_QUESTIONS_ASKED
    !insertmacro MUI_UNPAGE_WELCOME
    !insertmacro MUI_UNPAGE_CONFIRM
  !endif
  !insertmacro MUI_UNPAGE_INSTFILES


;--------------------------------
;Language jockeying

  ; Load up our localized messages
  !include "${RSRCDIR}\messages.nlf"

  Name "${NAME}"
!ifndef PUBLISHER
  InstallDir "$PROGRAMFILES\Three Rings Design\${INSTALL_DIR}"
!else
  InstallDir "$PROGRAMFILES\${PUBLISHER}\${INSTALL_DIR}"
!endif

  !define MUI_BRANDINGTEXT "$(branding)"
  !define MUI_DIRECTORYPAGE_TEXT_TOP "$(install_where)"


;--------------------------------
;Data

  !define REQUIRED_JREVERSION "1.5.0"
  !define JREVERSION "1.5.0"
  !define JREZIP "java.zip"

  AutoCloseWindow true  ; close the window when the install is done
  ShowInstDetails nevershow  ;hide  ;show
  ShowUninstDetails show
  XPStyle on


;-------------------------------------------------------------
Function .onInit

  ; install things only for the current user
  SetShellVarContext current

  ; Check to see if they already have the game installed
  ; Note: Prior to 04/04/07 we used to store our registry keys in HKLM, same location otherwise.
  ReadRegStr $R0 HKCU "SOFTWARE\$(company_name)\${NAME}" \
      INSTALL_DIR_REG_KEY
  StrCmp $R0 "" CheckInstallPrivs
  ClearErrors
  IfFileExists "$R0\$(shortcut_name).lnk" 0 CheckInstallPrivs
  !ifndef AUTORUN_INSTALLED
    Push $CMDLINE
    Push "/run"
    Call StrStr
    Pop $R1
    StrCmp $R1 "" 0 RunAlreadyInstalled
    MessageBox MB_YESNOCANCEL|MB_ICONQUESTION "$(already_installed)" \
        IDNO AskReinstall IDCANCEL Done
  !endif
  RunAlreadyInstalled:
  ExecShell "" "$R0\$(shortcut_name).lnk"
  Done:
  Quit

  AskReinstall:
  MessageBox MB_YESNO|MB_ICONQUESTION "$(reinstall)" \
    IDNO Done

  ; Check that they are installing with the necessary privileges
  CheckInstallPrivs:
  Call GetWindowsVersion
  Pop $R0
  ; If we're on Vista install to the user space
  StrCmp $R0 "Vista" UserPath

  Call IsUserAdmin
  Pop $R0
  StrCmp $R0 "true" ProceedInstall

  UserPath:
  ; Install in their home directory instead
  StrCpy $INSTDIR "$APPDATA\Three Rings Design\${INSTALL_DIR}"

  ProceedInstall:
  ClearErrors

FunctionEnd


;-------------------------------------------------------------
; Usage:
;   Call IsUserAdmin
;   Pop $R0   ; at this point $R0 is "true" or "false"
Function IsUserAdmin
  Push $R0
  Push $R1
  Push $R2

  ClearErrors
  UserInfo::GetName
  IfErrors Win9x
  Pop $R1
  UserInfo::GetAccountType
  Pop $R2

  StrCmp $R2 "Admin" 0 Continue

  ; Observation: I get here when running Win98SE. (Lilla)
  ; The functions UserInfo.dll looks for are there on Win98 too, but just
  ; don't work. So UserInfo.dll, knowing that admin isn't required on
  ; Win98, returns admin anyway. (per kichik)
  StrCpy $R0 "true"
  Goto Done

  Continue:
  ; You should still check for an empty string because the functions
  ; UserInfo.dll looks for may not be present on Windows 95. (per kichik)
  StrCmp $R2 "" Win9x
  StrCpy $R0 "false"
  Goto Done

  Win9x:
  ; comment/message below is by UserInfo.nsi author:
  ; This one means you don't need to care about admin or
  ; not admin because Windows 9x doesn't either
  StrCpy $R0 "true"

  Done:
  Pop $R2
  Pop $R1
  Exch $R0
FunctionEnd


;-------------------------------------------------------------
Section "Install" InstStuff
  ; add in the size the JRE once it is unpacked
  AddSize 100000

  ; create our installation directory
  ClearErrors
  CreateDirectory "$INSTDIR"
  IfErrors 0 CheckForJRE
     MessageBox MB_OK|MB_ICONSTOP "$(no_create_instdir)"
     Quit

  ; check for the necessary JRE
  CheckForJRE:
  ClearErrors
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Web Start" "CurrentVersion"
  IfErrors NeedsRequiredJRE
  Push ${REQUIRED_JREVERSION}
  Push $R0
  Call CompareVersions
  Pop $R0
  IntCmp $R0 1 LocateJRE NeedsRequiredJRE

  LocateJRE:
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" \
    "CurrentVersion"
  StrCmp $R0 "" NeedsRequiredJRE
  ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R0" \
    "JavaHome"
  StrCmp $R0 "" NeedsRequiredJRE
  IfFileExists "$R0\bin\java.exe" 0 NeedsRequiredJRE
    StrCpy $R8 $R0
    Goto HasRequiredJRE

  NeedsRequiredJRE:
  ; (maybe download and) install the JRE
  SetOutPath $TEMP
!ifdef BUNDLE_JVM
  File "${DATADIR}\${JREZIP}"
!endif

!ifndef BUNDLE_JVM
  !insertmacro MUI_HEADER_TEXT "$(dl_header)" "$(dl_subheader)"
  NSISdl::download "${HOST}/${JREZIP}" "$TEMP\${JREZIP}"
  Pop $R0 ;Get the return value
  StrCmp $R0 "success" ContinueWithJREInstall
    StrCmp $R0 "cancel" AbandonShip
      MessageBox MB_YESNO|MB_ICONQUESTION \
        "$(download_fail_pre)$\r$\r$R0$\r$\r$(download_fail_post)" \
        IDNO AbandonShip
      ExecShell open "${HOST}/jre_download_failed.html"
    AbandonShip:
    Quit
  ContinueWithJREInstall:
!endif

  ClearErrors
  ZipDLL::extractall "$TEMP\${JREZIP}" "$INSTDIR"
  Pop $R0 ;Get the return value
  StrCmp $R0 "success" NoteInstalledJRE
    ExecShell open "${HOST}/jre_download_failed.html"
    MessageBox MB_OK|MB_ICONSTOP "$(jre_error)"
    Quit

  NoteInstalledJRE:
  StrCpy $R8 "$INSTDIR\java"

  HasRequiredJRE:
  Delete $TEMP\${JREZIP}
  ClearErrors

  ; Install Getdown and the configuration
  SetOutPath $INSTDIR
  File "${DATADIR}\getdown-pro.jar"
  File "${DATADIR}\jRegistryKey.dll"
  File "${RSRCDIR}\background.png"
  File "${RSRCDIR}\progress.png"
  File "${RSRCDIR}\app_icon.ico"
  File "${RSRCDIR}\editor_icon.ico"
  File "${RSRCDIR}\viewer_icon.ico"

  !ifdef ASSUME_OFFLINE_INSTALL
    ; Create a blank proxy.txt file
    FileOpen $9 "$INSTDIR\proxy.txt" "w"
    FileClose $9
  !else
    ; Create our bootstrap getdown.txt file
    FileOpen $9 "$INSTDIR\getdown.txt" "w"
    FileWrite $9 "appbase = ${HOST}/client$\r$\n"
    FileClose $9
  !endif

  ; Create the affiliate id file
  !ifdef FORCED_COBRAND_ID
    StrCpy $R0 "bang-${FORCED_COBRAND_ID}-install.exe"
  !else
    System::Call 'kernel32::GetModuleFileNameA(i 0, t .R0, i 1024) i r1'
  !endif
  FileOpen $9 "$INSTDIR\installer.txt" "w"
  FileWrite $9 "$R0$\r$\n"
  FileClose $9

  ; Create our main launcher "shortcut"
  CreateShortCut "$INSTDIR\$(shortcut_name).lnk" \
                 "$R8\bin\javaw.exe" "-jar getdown-pro.jar ." \
                 "$INSTDIR\app_icon.ico" "" "" "" "$(shortcut_hint)"

  ; Create the editor launcher "shortcut"
  CreateShortCut "$INSTDIR\$(editor_shortcut_name).lnk" \
                 "$R8\bin\javaw.exe" "-jar getdown-pro.jar . editor" \
                 "$INSTDIR\editor_icon.ico" "" "" "" "$(tool_shortcut_hint)"

  ; Create the model viewer launcher "shortcut"
  CreateShortCut "$INSTDIR\$(viewer_shortcut_name).lnk" \
                 "$R8\bin\javaw.exe" "-jar getdown-pro.jar . viewer" \
                 "$INSTDIR\viewer_icon.ico" "" "" "" "$(tool_shortcut_hint)"

  ; Create the links to the home page and manual
  WriteINIStr "$INSTDIR\$(homepage_name).url" \
              "InternetShortcut" "URL" "${HOST}"
  WriteINIStr "$INSTDIR\$(manual_name).url" \
              "InternetShortcut" "URL" "${HOST}/docs/"

  ; Write the uninstaller
  WriteUninstaller "$INSTDIR\$(uninstaller_name)"

  ; Create shortcuts in the start menu and on the desktop
!ifdef PUBLISHER
  CreateDirectory "$SMPROGRAMS\${PUBLISHER}\${NAME}"
  CreateShortCut "$SMPROGRAMS\${PUBLISHER}\${NAME}\${NAME}.lnk" \
                 "$INSTDIR\$(shortcut_name).lnk"
  CreateShortCut "$SMPROGRAMS\${PUBLISHER}\${NAME}\$(manual_name).lnk" \
                 "$INSTDIR\$(manual_name).url"
  CreateShortCut "$SMPROGRAMS\${PUBLISHER}\${NAME}\$(uninstaller_link).lnk" \
                 "$INSTDIR\$(uninstaller_name)"
!else
  CreateShortCut "$SMPROGRAMS\${NAME}.lnk" \
                 "$INSTDIR\$(shortcut_name).lnk"
!endif
  CreateShortCut "$DESKTOP\${NAME}.lnk" \
                 "$INSTDIR\$(shortcut_name).lnk"

  ; Set up registry stuff
  WriteRegStr HKCU "SOFTWARE\$(company_name)\${NAME}" \
                   INSTALL_DIR_REG_KEY $INSTDIR
  WriteRegStr HKCU "SOFTWARE\$(company_name)\${NAME}" \
                   PRODUCT_VERSION_REG_KEY ${INSTALLER_VERSION}

  StrCpy $R0 "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${NAME}"
  WriteRegStr HKCU $R0 "DisplayName" "${NAME}"
  WriteRegStr HKCU $R0 "UninstallString" "$INSTDIR\$(uninstaller_name)"
  WriteRegDWORD HKCU $R0 "NoModify" 1
SectionEnd


;-------------------------------------------------------------
; Set up the uninstall
Section "Uninstall"
  SetShellVarContext current

  RMDir /r "$INSTDIR"
!ifdef PUBLISHER
  RMDir /r "$SMPROGRAMS\${PUBLISHER}\${NAME}"
  RMDir "$SMPROGRAMS\${PUBLISHER}"
!else
  RMDir /r "$SMPROGRAMS\${NAME}"
!endif
  Delete "$DESKTOP\${NAME}.lnk"
  DeleteRegKey HKCU "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${NAME}"
  DeleteRegKey HKCU "SOFTWARE\$(company_name)\${NAME}"
  DeleteRegKey HKCU "SOFTWARE\$(company_name)"
SectionEnd


Function .onInstSuccess
  ; Now run Getdown which will download everything else and finish the job
  IfFileExists "$INSTDIR\$(shortcut_name).lnk" 0 itfailed
    ExecShell "" "$INSTDIR\$(shortcut_name).lnk"
  itfailed:
FunctionEnd


;-------------------------------------------------------------------------------
 ; CompareVersions
 ; input:
 ;    top of stack = existing version
 ;    top of stack-1 = needed version
 ; output:
 ;    top of stack = 1 if current version => neded version, else 0
 ; version is a string in format "xx.xx.xx.xx" (number of interger sections
 ; can be different in needed and existing versions)

Function CompareVersions
   ; stack: existing ver | needed ver
   Exch $R0
   Exch
   Exch $R1
   ; stack: $R1|$R0

   Push $R1
   Push $R0
   ; stack: e|n|$R1|$R0

   ClearErrors
   loop:
      IfErrors VersionNotFound
      Strcmp $R0 "" VersionTestEnd

      Call ParseVersion
      Pop $R0
      Exch

      Call ParseVersion
      Pop $R1
      Exch

      IntCmp $R1 $R0 +1 VersionOk VersionNotFound
      Pop $R0
      Push $R0

   goto loop

   VersionTestEnd:
      Pop $R0
      Pop $R1
      Push $R1
      Push $R0
      StrCmp $R0 $R1 VersionOk VersionNotFound

   VersionNotFound:
      StrCpy $R0 "0"
      Goto end

   VersionOk:
      StrCpy $R0 "1"
end:
   ; stack: e|n|$R1|$R0
   Exch $R0
   Pop $R0
   Exch $R0
   ; stack: res|$R1|$R0
   Exch
   ; stack: $R1|res|$R0
   Pop $R1
   ; stack: res|$R0
   Exch
   Pop $R0
   ; stack: res
FunctionEnd

;-----------------------------------------------------------------------------
 ; ParseVersion
 ; input:
 ;      top of stack = version string ("xx.xx.xx.xx")
 ; output:
 ;      top of stack   = first number in version ("xx")
 ;      top of stack-1 = rest of the version string ("xx.xx.xx")
Function ParseVersion
   Exch $R1 ; version
   Push $R2
   Push $R3

   StrCpy $R2 1
   loop:
      StrCpy $R3 $R1 1 $R2
      StrCmp $R3 "." loopend
      StrLen $R3 $R1
      IntCmp $R3 $R2 loopend loopend
      IntOp $R2 $R2 + 1
      Goto loop
   loopend:
   Push $R1
   StrCpy $R1 $R1 $R2
   Exch $R1

   StrLen $R3 $R1
   IntOp $R3 $R3 - $R2
   IntOp $R2 $R2 + 1
   StrCpy $R1 $R1 $R3 $R2

   Push $R1

   Exch 2
   Pop $R3

   Exch 2
   Pop $R2

   Exch 2
   Pop $R1
FunctionEnd

;-----------------------------------------------------------------------------
 ; StrStr
 ; input:
 ;      top of stack   = string to search for (the needle)
 ;      top of stack-1 = string to search in (the haystack)
 ; output:
 ;      top of stack   = replaces with the portion of the string remaining
 ;
 ; Usage:
 ;   Push "this is a long ass string"
 ;   Push "ass"
 ;   Call StrStr
 ;   Pop $R0
 ;  ($R0 at this point is "ass string")

Function StrStr
   Exch $R1 ; st=haystack,old$R1, $R1=needle
   Exch     ; st=old$R1,haystack
   Exch $R2 ; st=old$R1,old$R2, $R2=haystack
   Push $R3
   Push $R4
   Push $R5
   StrLen $R3 $R1
   StrCpy $R4 0
   ; $R1=needle
   ; $R2=haystack
   ; $R3=len(needle)
   ; $R4=cnt
   ; $R5=tmp
   loop:
     StrCpy $R5 $R2 $R3 $R4
     StrCmp $R5 $R1 done
     StrCmp $R5 "" done
     IntOp $R4 $R4 + 1
     Goto loop
   done:
   StrCpy $R1 $R2 "" $R4
   Pop $R5
   Pop $R4
   Pop $R3
   Pop $R2
   Exch $R1
FunctionEnd

;-----------------------------------------------------------------------------
 ; GetWindowsVersion
 ;
 ; Based on Yazno's function, http://yazno.tripod.com/powerpimpit/
 ; Updated by Joost Verburg
 ;
 ; Returns on top of stack
 ;
 ; Windows Version (95, 98, ME, NT x.x, 2000, XP, 2003, Vista)
 ; or
 ; '' (Unknown Windows Version)
 ;
 ; Usage:
 ;   Call GetWindowsVersion
 ;   Pop $R0
 ;   ; at this point $R0 is "NT 4.0" or whatnot

 Function GetWindowsVersion

   Push $R0
   Push $R1

   ClearErrors

   ReadRegStr $R0 HKLM \
   "SOFTWARE\Microsoft\Windows NT\CurrentVersion" CurrentVersion

   IfErrors 0 lbl_winnt

   ; we are not NT
   ReadRegStr $R0 HKLM \
   "SOFTWARE\Microsoft\Windows\CurrentVersion" VersionNumber

   StrCpy $R1 $R0 1
   StrCmp $R1 '4' 0 lbl_error

   StrCpy $R1 $R0 3

   StrCmp $R1 '4.0' lbl_win32_95
   StrCmp $R1 '4.9' lbl_win32_ME lbl_win32_98

   lbl_win32_95:
     StrCpy $R0 '95'
   Goto lbl_done

   lbl_win32_98:
     StrCpy $R0 '98'
   Goto lbl_done

   lbl_win32_ME:
     StrCpy $R0 'ME'
   Goto lbl_done

   lbl_winnt:

   StrCpy $R1 $R0 1

   StrCmp $R1 '3' lbl_winnt_x
   StrCmp $R1 '4' lbl_winnt_x

   StrCpy $R1 $R0 3

   StrCmp $R1 '5.0' lbl_winnt_2000
   StrCmp $R1 '5.1' lbl_winnt_XP
   StrCmp $R1 '5.2' lbl_winnt_2003
   StrCmp $R1 '6.0' lbl_winnt_vista lbl_error

   lbl_winnt_x:
     StrCpy $R0 "NT $R0" 6
   Goto lbl_done

   lbl_winnt_2000:
     Strcpy $R0 '2000'
   Goto lbl_done

   lbl_winnt_XP:
     Strcpy $R0 'XP'
   Goto lbl_done

   lbl_winnt_2003:
     Strcpy $R0 '2003'
   Goto lbl_done

   lbl_winnt_vista:
     Strcpy $R0 'Vista'
   Goto lbl_done

   lbl_error:
     Strcpy $R0 ''
   lbl_done:

   Pop $R1
   Exch $R0

 FunctionEnd

; eof
