#
# $Id$
#
# Proguard configuration file for Game Gardens client

-injars lib/commons-io.jar(!META-INF/*)
-injars lib/getdown.jar(!META-INF/*,!**/tools/**)
-injars lib/narya-base.jar(!META-INF/*,!**/tools/**)
-injars lib/narya-distrib.jar(!META-INF/*,!**/tools/**)
-injars lib/narya-media.jar(!META-INF/*,!**/tools/**)
-injars lib/narya-parlor.jar(!META-INF/*,!**/tools/**)
-injars lib/samskivert.jar(!META-INF/*,!**/velocity/**,!**/xml/**)
-injars lib/toybox.jar(!META-INF/*,!**/tools/**,!**/xml/**)
-injars dist/bang.jar(!META-INF/*)

-libraryjars <java.home>/lib/rt.jar

-dontobfuscate

-outjars dist/bang-editor.jar

# we need whatever we keep of samskivert to be around in its entirety so
# that if a game uses the same classfile, the whole thing is there
-keep public class com.samskivert.Log {
    public protected *;
}
-keep public class com.samskivert.io.** {
    public protected *;
}
-keep public class com.samskivert.net.AttachableURLFactory {
    public protected *;
}
-keep public class com.samskivert.net.PathUtil {
    public protected *;
}
-keep public class com.samskivert.servlet.user.Password {
    public protected *;
}
-keep public class com.samskivert.servlet.user.UserUtil {
    public protected *;
}
-keep public class com.samskivert.swing.** {
    public protected *;
}
-keep public class com.samskivert.text.MessageUtil {
    public protected *;
}
-keep public class com.samskivert.util.** {
    public protected *;
}

# similarly for all of the narya libraries
-keep public class com.threerings.** {
    public protected *;
}
