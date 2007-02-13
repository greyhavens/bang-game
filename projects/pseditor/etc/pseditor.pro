#
# $Id$
#
# Proguard configuration file for Game Gardens client

-injars ../dist/lib/jme.jar(!META-INF/*)
-injars ../dist/lib/jme-effects.jar(!META-INF/*)
-injars ../dist/lib/jme-awt.jar(!META-INF/*)
-injars ../dist/lib/jme-gamestates.jar(!META-INF/*)
-injars ../dist/lib/jme-scene.jar(!META-INF/*)
-injars ../dist/pseditor.jar(!META-INF/*)

-libraryjars ../dist/lib/lwjgl.jar
-libraryjars <java.home>/lib/rt.jar

-dontobfuscate

-outjars ../dist/pseditor-pro.jar

-keep public class * implements com.jme.util.export.Savable {
    *;
}

-keep public class jmetest.effects.RenParticleEditor {
    public static void main (java.lang.String[]);
}
