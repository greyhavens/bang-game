//
// $Id$

package com.threerings.bang.client;

import com.jme3.system.AppSettings;

import com.threerings.bang.client.BangPrefs;

/**
 * The client entry point. jME3 cutover (Phase 3 — the atomic host flip): {@link BangApp} is now a
 * {@code com.jme3.app.SimpleApplication} on the LWJGL3 context. This launcher configures the
 * display from {@link BangPrefs} and starts the jME3 main loop (which creates the LWJGL3 window,
 * GL context and input).
 */
public class BangDesktop
{
    public static void main (String[] args)
    {
        final BangApp app = new BangApp();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("Bang! Howdy");
        // seed width/height/bpp/freq/fullscreen from stored preferences (clamped to the minimum
        // playable size); -Dsafemode=true forces the minimum windowed mode for recovery.
        BangPrefs.configureDisplayMode(settings, Boolean.getBoolean("safemode"));
        settings.setVSync(true);
        // request a depth buffer for the 3D scene.
        settings.setDepthBits(24);

        // BUG-1: create the GLFW window resizable (so the OS gives it a maximize button and lets
        // the user drag-resize / maximize) and centered. Without this the LWJGL3 context defaults
        // to a fixed, non-resizable window. jME3's RenderManager.notifyReshape (driven by the GLFW
        // framebuffer-size callback) re-fits every viewport camera on resize, and our BUI
        // SceneProcessor reads the GUI viewport size each frame, so resizing "just works" visually.
        settings.setResizable(true);
        settings.setCenterWindow(true);
        // hint a minimum playable size (advisory on LWJGL3; the resolution chooser enforces it).
        settings.setMinResolution(BangUI.MIN_WIDTH, BangUI.MIN_HEIGHT);

        app.setSettings(settings);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);

        // route external termination (SIGTERM/SIGINT) through an ordered jME3 stop so the GL
        // context and the OpenAL device are torn down on the render thread before the JVM begins
        // shutting down (openal-soft's atexit cleanup can otherwise segfault against its own mixer
        // thread).
        sun.misc.SignalHandler exiter = sig -> {
            app.stop();
            Thread enforcer = new Thread("BangDesktop exit enforcer") {
                @Override public void run () {
                    try {
                        Thread.sleep(5000L);
                    } catch (InterruptedException ie) {
                        return;
                    }
                    Runtime.getRuntime().halt(1);
                }
            };
            enforcer.setDaemon(true);
            enforcer.start();
        };
        sun.misc.Signal.handle(new sun.misc.Signal("TERM"), exiter);
        sun.misc.Signal.handle(new sun.misc.Signal("INT"), exiter);

        app.start();
    }
}
