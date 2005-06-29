;
; $Id: yohoho-installer.nsi 18205 2004-12-06 19:25:44Z ray $
;
; Bang! Howdy Installer for Windows x86

  !define LOCALE "en"
  !define DEPLOYMENT "client"
  !define NAME "Bang! Howdy"
  !define INSTALL_DIR "Bang Howdy"
  !define HOST "http://dev.banghowdy.com"
  !ifndef OUTFILENAME
    !define OUTFILENAME "bang-install.exe"
  !endif

  ; comment this out to enable the code that automatically downloads
  ; the JVM from the web and installs it
  ; !define BUNDLE_JVM true

  !define NO_QUESTIONS_ASKED   ; quicky installer
  !include "..\data\installer-common.nsi"

Section "Extra" ExtraStuff
  ; add the size of the uncompressed and compressed art and code
  AddSize 100000
SectionEnd
