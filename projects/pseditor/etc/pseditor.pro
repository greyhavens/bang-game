#
# $Id$
#
# Proguard configuration file for particle system editor

-dontobfuscate

-libraryjars <java.home>/lib/rt.jar

-dontwarn com.jme.system.lwjgl.LWJGLSystemProvider
-dontwarn com.jmex.font2d.**
-dontwarn com.jmex.sound.openAL.objects.util.*
-dontwarn com.jmex.terrain.*
-dontwarn net.java.games.input.**

-keep public class * implements com.jme.util.export.Savable {
    *;
}

-keep public class jmetest.effects.RenParticleEditor {
    public static void main (java.lang.String[]);
}
