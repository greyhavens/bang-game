//
// $Id$

package com.threerings.bang.editor;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.google.inject.Injector;
import com.google.inject.Guice;

import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;

import com.threerings.bang.client.BangApp;

/**
 * The board editor entry point. jME3 cutover (Phase 7b — editor canvas embedding): {@link EditorApp}
 * is a {@code com.jme3.app.SimpleApplication}; instead of opening its own LWJGL3 window we drive the
 * jME3 Canvas context ({@code JmeContext.Type.Canvas}) and embed the resulting {@link java.awt.Canvas}
 * in the center of a Swing {@link JFrame}, with the editor's Swing chrome (menu bar, status bar, and
 * the {@code EditorPanel} tool/info panels) laid out around it. This is the embedding seam the fork
 * editor used (a gdx {@code LwjglCanvas} in a {@code JFrame}); the keystone was proven by
 * {@code tools/jme3-host}'s CanvasSpike in 7a.
 *
 * <p>Ordering matters: {@link EditorApp#create()} runs {@code _client.init(this, frame)} +
 * {@code _client.start()} from the jME3 init thread once {@link com.jme3.app.SimpleApplication#startCanvas}
 * spins the GL loop, so {@code app.frame}/{@code app.canvas} must both be set (and the frame visible,
 * so the canvas has a real size) BEFORE {@code startCanvas()}.
 */
public class EditorDesktop
{
    public static void main (String[] args)
    {
        // configure our debug log
        BangApp.configureLog("editor.log");

        // save these for later
        EditorApp.appArgs = args;

        // create our editor server which we're going to run in the same JVM with the client
        Injector injector = Guice.createInjector(new EditorServer.Module());
        EditorServer server = injector.getInstance(EditorServer.class);
        try {
            server.init(injector);
        } catch (Exception e) {
            System.err.println("Unable to initialize server.");
            e.printStackTrace(System.err);
        }

        // let the BangClientController know we're in editor mode
        System.setProperty("editor", "true");

        final EditorApp app = injector.getInstance(EditorApp.class);

        // LWJGL3 AppSettings for the Canvas context (matches the proven CanvasSpike path).
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Bang Editor");
        settings.setWidth(WIDTH);
        settings.setHeight(HEIGHT);
        settings.setRenderer(AppSettings.LWJGL_OPENGL2);  // = "LWJGL-OpenGL2", the LWJGL3 backend
        settings.setVSync(true);
        settings.setDepthBits(24);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);

        // build the Canvas context (does NOT start the GL loop yet); on LWJGL3 this yields a
        // com.jme3.system.lwjgl.LwjglCanvas (implements JmeCanvasContext).
        app.createCanvas();
        JmeCanvasContext ctx = (JmeCanvasContext)app.getContext();
        final Canvas canvas = ctx.getCanvas();
        canvas.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        // publish the canvas to the app so EditorPanel.willEnterPlace's sizeToCanvas can find it
        // (it ComponentListens on EditorApp.getCanvas() to size the BUI board-view window).
        app.canvas = canvas;

        // build the Swing chrome on the EDT and publish the frame to the app BEFORE startCanvas()
        // (EditorApp.create -> EditorClient.init(app, app.frame) reads it from the render thread).
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run () {
                    // heavyweight GL canvas + lightweight Swing popups don't mix.
                    JPopupMenu.setDefaultLightWeightPopupEnabled(false);

                    JFrame frame = new JFrame("Bang Editor");
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.getContentPane().setLayout(new BorderLayout());
                    // the GL canvas fills the center; EditorClient.init adds the SOUTH status bar +
                    // menu bar, and EditorContextImpl.setPlaceView adds the EditorPanel chrome EAST.
                    frame.getContentPane().add(canvas, BorderLayout.CENTER);
                    frame.setSize(WIDTH, HEIGHT);
                    frame.setLocationRelativeTo(null);
                    app.frame = frame;
                    frame.setVisible(true);
                }
            });
        } catch (Exception e) {
            System.err.println("Unable to build editor frame.");
            e.printStackTrace(System.err);
            return;
        }

        // spin the GL render loop against the canvas context; simpleInitApp (and our create()
        // hook) fire on the resulting jME3 render thread.
        app.startCanvas();
    }

    protected static final int WIDTH = 1224, HEIGHT = 768;
}
