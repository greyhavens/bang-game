#!/bin/sh
#
# $Id: finish_install.sh 18776 2005-01-22 00:23:14Z mdb $
#
# Completes the installation of the Bang! game launcher.

echo
echo "-------------------------------------------------------------------------"
echo " Welcome to the @client_title@ installer!"
echo "-------------------------------------------------------------------------"

echo
echo "Please press enter to view the license agreement."
read

# display the license agreement and require compliance
more < license.txt
AGREED=
while [ -z "$AGREED" ]; do
    echo
    echo "Do you agree to the above license terms? [yes or no] "
    read REPLY IGNORED_EXTRA
    case $REPLY in
        yes | Yes | YES)
            AGREED=1
            ;;
        no | No | NO)
            echo "Well pardner, if you don't like the terms, ya can't play.";
            exit 1;
            ;;
        *)
            echo "You must type 'yes' or 'no', pardner."
            ;;
    esac
done

# ask them which java installation to use
DEFJAVADIR=$JAVA_HOME
if [ \! -x $DEFJAVADIR/bin/java ]; then
    DEFJAVADIR=`which java`
    if [ -x $DEFJAVADIR ]; then
        DEFJAVADIR=`echo $JAVADIR | sed 's:/bin/java::g'`
    fi
fi
JAVADIR=
while [ -z "$JAVADIR" ]; do
    echo
    echo "Which Java Virtual Machine would you like to use?"
    echo "Note: the JVM must be version 1.5.0 or newer."
    echo -n "[$DEFJAVADIR] "
    read REPLY
    if [ -z "$REPLY" ]; then
        REPLY=$DEFJAVADIR
    fi
    if [ \! -x $REPLY/bin/java ]; then
        echo "Could not locate '$REPLY/bin/java'."
        echo "Please ensure that you entered the proper path."
    else
        JAVADIR=$REPLY
    fi
done

# ask them where they want to install the game
DEFINSTALLDIR=$HOME/@client_ident@
if [ "@client_ident@" = "client" ]; then
    # make things pretty for the default installation
    DEFINSTALLDIR=$HOME/bang
elif [ "@client_ident@" = "tclient" ]; then
    # make things pretty for the test installation
    DEFINSTALLDIR=$HOME/bang_test
fi
INSTALLDIR=
while [ -z "$INSTALLDIR" ]; do
    echo
    echo "Where would you like to install @client_ident@?"
    echo -n "[$DEFINSTALLDIR] "
    read REPLY
    if [ -z "$REPLY" ]; then
        REPLY=$DEFINSTALLDIR
    fi
    if [ \! -d $REPLY ]; then
        echo "Creating directory '$REPLY'..."
        mkdir -p $REPLY
        if [ \! -d $REPLY ]; then
            echo "Unable to create directory '$REPLY'."
        else
            INSTALLDIR=$REPLY
            break
        fi
    else
        INSTALLDIR=$REPLY
        break
    fi
done

# copy our files to the install directory
cp -p * $INSTALLDIR
rm $INSTALLDIR/finish_install.sh

# we were passed the file name of the installer script as our first
# argument, write that to a file in the installation directory
echo "$1" > $INSTALLDIR/installer.txt

# set up the symlink pointing to the desired java installation
rm -f  $INSTALLDIR/java
ln -s $JAVADIR $INSTALLDIR/java

# attempt to locate their desktop directory
DESKTOP=$HOME/Desktop
if [ \! -d $DESKTOP ]; then
    DESKTOP=$HOME/.desktop
fi
if [ \! -d $DESKTOP ]; then
    DESKTOP=$INSTALLDIR
    echo
    echo "Note: Unable to locate your desktop directory. Please move"
    echo "'$DESKTOP/@client_title@.desktop' to your desktop"
    echo "directory if you wish to launch Bang! from a desktop icon."
fi

cat > "$DESKTOP/@client_title@.desktop" <<EOF
[Desktop Entry]
Name=@client_title@
Exec=$INSTALLDIR/bang
Icon=$INSTALLDIR/desktop.png
Terminal=false
MultipleArgs=false
Type=Application
Categories=Application;
EOF

cat > "$DESKTOP/@editor_title@.desktop" <<EOF
[Desktop Entry]
Name=@editor_title@
Exec=$INSTALLDIR/bangeditor
Icon=$INSTALLDIR/desktop.png
Terminal=false
MultipleArgs=false
Type=Application
Categories=Application;
EOF

echo
echo "-------------------------------------------------------------------------"
echo "@client_title@ has been successfully installed!"
echo "Use $INSTALLDIR/bang or the desktop icon to run it."
echo
echo "If you wish to uninstall @client_title@ later, simply delete the"
echo "$INSTALLDIR directory."
echo "-------------------------------------------------------------------------"
