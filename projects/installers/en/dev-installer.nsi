;
; $Id$
;
; Bang! Howdy Dev Installer for Windows x86

  !define LOCALE "en"
  !define DEPLOYMENT "devclient"
  !define NAME "Bang! Howdy (Dev)"
  !define INSTALL_DIR "Bang Howdy Dev"
  !define HOST "http://download.earth.threerings.net/bang"
  !ifndef OUTFILENAME
    !define OUTFILENAME "..\dist\dev-install.exe"
  !endif

  ; comment this out to enable the code that automatically downloads
  ; the JVM from the web and installs it
  ; !define BUNDLE_JVM true

  !include "..\data\installer-common.nsi"

Section "Extra" ExtraStuff
  ; add the size of the uncompressed and compressed art and code
  AddSize 100000
SectionEnd
