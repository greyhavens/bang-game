;
; $Id: dev-installer.nsi 18205 2004-12-06 19:25:44Z ray $
;
; Bang! Howdy Dev Installer for Windows x86

  !define LOCALE "en"
  !define DEPLOYMENT "client"
  !define NAME "Bang! Howdy (Dev)"
  !define INSTALL_DIR "${NAME}"
  !define HOST "http://dev.banghowdy.com"
  !define OUTFILENAME "dev-install.exe"

  !include "..\data\installer-common.nsi"

Section "Extra" ExtraStuff
  ; add the size of the uncompressed and compressed art and code
  AddSize 100000
SectionEnd
