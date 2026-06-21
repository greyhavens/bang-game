//
// Phase 7a keystone SPIKE -- THROWAWAY. Proves jME3 3.9 can render into a Swing JFrame via an
// embedded AWT Canvas on the LWJGL3 backend. This is the make-or-break for the whole editor
// (7b/7c): EditorApp has `JFrame frame` + `Canvas canvas` fields that were never wired, and the
// fork editor used to embed a gdx LwjglCanvas in a JFrame. The jME3 equivalent is the
// JmeContext.Type.Canvas path: SimpleApplication.createCanvas() builds a JmeCanvasContext whose
// getCanvas() returns a java.awt.Canvas we drop into a Swing panel; startCanvas() spins the GL
// loop. On LWJGL3 the canvas context is com.jme3.system.lwjgl.LwjglCanvas (jme3-lwjgl3), which
// bridges to org.lwjgl.opengl.awt.AWTGLCanvas from org.lwjglx:lwjgl3-awt.
//
// Run:  ./gradlew :tools:jme3-host:runCanvasSpike        (needs DISPLAY=:1, a real GL context)
// It opens a JFrame with a spinning cube + a Swing toolbar (to prove Swing chrome coexists with
// the GL canvas), grabs a framebuffer screenshot, and self-terminates.

package com.threerings.bang.tools.jme3host;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;
import com.jme3.system.JmeContext;

/**
 * Standalone spike: a jME3 spinning cube rendered into a Swing {@link JFrame} via an embedded
 * AWT {@link java.awt.Canvas} on the LWJGL3 backend ({@link JmeContext.Type#Canvas}).
 */
public class CanvasSpike extends SimpleApplication
{
    public static void main (String[] args)
    {
        final CanvasSpike app = new CanvasSpike();

        // 1. settings: LWJGL3 OpenGL renderer (the only renderer on this branch's classpath).
        AppSettings settings = new AppSettings(true);
        settings.setTitle("jME3-in-Swing canvas spike");
        settings.setWidth(CANVAS_W);
        settings.setHeight(CANVAS_H);
        settings.setRenderer(AppSettings.LWJGL_OPENGL2);  // = "LWJGL-OpenGL2", LWJGL3 backend
        settings.setAudioRenderer(null);                  // no OpenAL device needed
        app.setSettings(settings);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);

        // 2. build the Canvas context (does NOT start the GL loop yet). On LWJGL3 this yields a
        //    com.jme3.system.lwjgl.LwjglCanvas (implements JmeCanvasContext).
        app.createCanvas();
        JmeCanvasContext ctx = (JmeCanvasContext)app.getContext();
        final java.awt.Canvas canvas = ctx.getCanvas();
        canvas.setPreferredSize(new Dimension(CANVAS_W, CANVAS_H));
        System.err.println("[spike] context class = " + ctx.getClass().getName());
        System.err.println("[spike] canvas class  = " + canvas.getClass().getName());

        // 3. Swing chrome on the EDT: a JFrame with a left toolbar (Swing) + the GL canvas
        //    (heavyweight) in the center -- exactly the editor's ToolPanel-around-3D-view shape.
        SwingUtilities.invokeLater(new Runnable() {
            public void run () {
                JFrame frame = new JFrame("Bang editor canvas spike (jME3 + lwjgl3-awt)");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                // heavyweight GL canvas + lightweight Swing popups don't mix:
                javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(false);

                JPanel tools = new JPanel();
                tools.setLayout(new BoxLayout(tools, BoxLayout.Y_AXIS));
                tools.add(new JLabel("Tool panel"));
                tools.add(new JButton("Place"));
                tools.add(new JButton("Brush"));
                tools.add(new JButton("Camera"));
                tools.setPreferredSize(new Dimension(140, CANVAS_H));

                frame.getContentPane().setLayout(new BorderLayout());
                frame.getContentPane().add(tools, BorderLayout.WEST);
                frame.getContentPane().add(canvas, BorderLayout.CENTER);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });

        // 4. start the GL render loop against the canvas context.
        app.startCanvas();
    }

    @Override
    public void simpleInitApp ()
    {
        flyCam.setEnabled(false);
        viewPort.setBackgroundColor(new ColorRGBA(0.12f, 0.15f, 0.22f, 1f));

        Box b = new Box(1, 1, 1);
        _cube = new Geometry("Box", b);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.95f, 0.75f, 0.2f, 1f));
        _cube.setMaterial(mat);
        rootNode.attachChild(_cube);

        cam.setLocation(new Vector3f(0, 0, 6));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        _shotter = new ScreenshotAppState(System.getProperty("shot.dir", "/tmp") + "/", "jme3-canvas-spike");
        stateManager.attach(_shotter);
        System.err.println("[spike] simpleInitApp done -- GL context is live inside the Swing canvas.");
    }

    @Override
    public void simpleUpdate (float tpf)
    {
        if (_cube != null) {
            _cube.rotate(0.6f * tpf, 0.9f * tpf, 0f);
        }
        _frames++;
        if (_frames == SHOT_FRAME) {
            _shotter.takeScreenshot();
        }
        if (_frames >= EXIT_AFTER_FRAMES) {
            stop();           // tears down the GL loop; the JFrame's EXIT_ON_CLOSE handles the rest
            System.exit(0);
        }
    }

    protected Geometry _cube;
    protected ScreenshotAppState _shotter;
    protected int _frames;

    protected static final int CANVAS_W = 800, CANVAS_H = 600;
    protected static final int SHOT_FRAME = 60;
    protected static final int EXIT_AFTER_FRAMES = 600;   // ~10s @ 60fps, self-terminating
}
