#
# $Id$
#
# Proguard configuration file for Bang! development client

-injars lib/commons-io.jar(!META-INF/*)
-injars lib/getdown.jar(!META-INF/*,!**/tools/**)
-injars lib/narya-base.jar(!META-INF/*,!**/tools/**)
-injars lib/narya-distrib.jar(!META-INF/*,!**/tools/**)
-injars lib/narya-media.jar(!META-INF/*,!**/tools/**)
-injars lib/narya-parlor.jar(!META-INF/*,!**/tools/**)
-injars lib/narya-jme.jar(!META-INF/*,!**/tools/**)
-injars lib/samskivert.jar(!META-INF/*,!**/velocity/**,!**/xml/**)
-injars lib/jme.jar(!META-INF/*)
-injars lib/jme-effects.jar(!META-INF/*)
-injars lib/jme-model.jar(!META-INF/*)
-injars lib/jme-terrain.jar(!META-INF/*)
-injars lib/jme-bui.jar(!META-INF/*)
-injars dist/bang.jar(!META-INF/*)

-libraryjars <java.home>/lib/rt.jar
-libraryjars lib/lwjgl.jar
-libraryjars lib/lwjgl_fmod3.jar

-dontoptimize
-dontobfuscate

-outjars dist/bang-client.jar

-keep public class * extends com.threerings.presents.dobj.DObject {
    !static !transient <fields>;
}
-keep public class * implements com.threerings.io.Streamable {
    !static !transient <fields>;
    public void readObject (com.threerings.io.ObjectInputStream);
    public void writeObject (com.threerings.io.ObjectOutputStream);
}

-keep public class com.threerings.bang.client.BangApp {
    public static void main (java.lang.String[]);
}
