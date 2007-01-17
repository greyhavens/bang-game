;
; $Id$
;
; Bang! Howdy Installer for Windows x86

  !define LOCALE "en"
  !define DEPLOYMENT "client"
  !define NAME "Bang! Howdy"
  !define INSTALL_DIR "Bang Howdy"
  !define HOST "http://download.threerings.net/bang"
  !ifndef OUTFILENAME
    !define OUTFILENAME "..\dist\bang-install.exe"
  !endif

  ; comment this out to enable the code that automatically downloads
  ; the JVM from the web and installs it
  ; !define BUNDLE_JVM true

  !include "..\data\installer-common.nsi"

Section "Extra" ExtraStuff
  ; add the size of the uncompressed and compressed art and code
  AddSize 100000
SectionEnd
