//
// Phase 5 step 3 proof harness -- ISOLATED jME3 host for BUI's jME3 render backend.
//

package com.threerings.bang.tools.jme3host;

import java.io.StringReader;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BStyleSheet;
import com.jmex.bui.BWindow;
import com.jmex.bui.backend.BackendProvider;
import com.jmex.bui.backend.Jme3RenderBackend;
import com.jmex.bui.layout.GroupLayout;

/**
 * An isolated jME3 ({@code SimpleApplication}, LWJGL3) host that proves BUI's jME3 render
 * backend ({@link Jme3RenderBackend}) actually renders. See docs/jme3-bui-host.md.
 *
 * <p>What it does each frame:
 * <ol>
 *   <li>installs {@link Jme3RenderBackend} via {@link BackendProvider#set} with this app's
 *       {@code AssetManager} (so the backend can load {@code Common/MatDefs/Misc/Unshaded.j3md}
 *       and create textures for BUI's AWT-rendered text/images);</li>
 *   <li>builds a small BUI scene: a {@link BWindow} (solid background + 2px border) holding a
 *       {@link BLabel}, a {@link BButton}, and a {@link BScrollPane} whose tall child is
 *       clipped -- exercising the scissor/clip path in
 *       {@link Jme3RenderBackend#intersectScissorBox};</li>
 *   <li>in {@code simpleRender}, brackets BUI's window traversal with
 *       {@code beginFrame(rm, w, h)} / {@code endFrame()}.</li>
 * </ol>
 *
 * <p>BUI's screen space has origin at the BOTTOM-LEFT (y up), which matches jME3's
 * {@code Renderer.setClipRect} and the GUI bucket's ortho projection, so no y-flip is needed
 * for primitive placement.
 */
public class BuiHostApp extends SimpleApplication
{
    public static void main (String[] args)
    {
        BuiHostApp app = new BuiHostApp();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("BUI on jME3 -- backend proof");
        settings.setResolution(WIDTH, HEIGHT);
        settings.setVSync(true);
        // headless-friendly: still needs a real GL context (the proof is a screenshot), but
        // keep the audio renderer off so we don't require an OpenAL device on the CI box.
        settings.setAudioRenderer(null);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.setPauseOnLostFocus(false);
        app.start();
    }

    @Override
    public void simpleInitApp ()
    {
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);

        // 1. install the jME3 BUI backend BEFORE building any widget (BImage's backing is
        //    chosen at construction time from BackendProvider.get()).
        _backend = new Jme3RenderBackend(assetManager);
        BackendProvider.set(_backend);

        // 2. build the BUI scene.
        _root = new BuiRootNode();
        try {
            _window = buildWindow();
        } catch (java.io.IOException ioe) {
            throw new RuntimeException("Failed to build BUI scene", ioe);
        }
        _root.addWindow(_window);

        // 3. render BUI through a SceneProcessor on the GUI viewport (the design in
        //    docs/jme3-bui-port.md). postQueue runs with the GUI viewport's ORTHO camera
        //    active and renders into the GUI viewport's framebuffer -- exactly BUI's pixel,
        //    bottom-left-origin coordinate space -- so no camera swap or y-flip is needed.
        guiViewPort.addProcessor(new BuiProcessor());

        // a framebuffer screenshot grabber (writes a PNG straight from GL, independent of any
        // X11 grab timing) -- the authoritative proof artifact. Attached to the GUI viewport
        // AFTER the BUI processor so its postFrame grab sees BUI already drawn.
        _shotter = new ScreenshotAppState(System.getProperty("shot.dir", "/tmp") + "/", "jme3-bui-fb");
        stateManager.attach(_shotter);
    }

    /** Builds a window exercising solid fill, border outline, text (label + button), and a
     * scroll pane (scissor/clip path). */
    protected BWindow buildWindow ()
        throws java.io.IOException
    {
        // A self-contained stylesheet: solid backgrounds + borders only (no image assets), so
        // the proof depends on nothing outside the bui jar. Text uses AWTTextFactory (the
        // DefaultResourceProvider wires it for every "font:" rule) -> a BufferedImage ->
        // jME3 texture, exercising Jme3ImageBacking end to end.
        String bss =
            "root { color: #FFFFFF; font: \"Dialog\" plain 18; }\n" +
            "window { background: solid #224466EE; border: 2 solid #FFCC33; padding: 12; }\n" +
            "label { color: #FFFFFF; font: \"Dialog\" bold 22; }\n" +
            "scrolllabel { color: #CCFFCC; font: \"Dialog\" plain 18; }\n" +
            "button { background: solid #883322; border: 1 solid #FFFFFF; padding: 6 14; " +
                "color: #FFFFFF; font: \"Dialog\" bold 20; text-align: center; }\n" +
            "scrollcontent { background: solid #112233; border: 1 solid #66AACC; }\n";

        BStyleSheet style = new BStyleSheet(
            new StringReader(bss), new BStyleSheet.DefaultResourceProvider());

        BWindow window = new BWindow(style, GroupLayout.makeVStretch());
        ((GroupLayout)window.getLayoutManager()).setGap(10);

        window.add(new BLabel("BUI on jMonkeyEngine 3.9", "label"));
        window.add(new BButton("Click Me"));

        // a tall column of labels inside a scroll pane: most rows fall outside the pane's
        // viewport and MUST be clipped by the scissor path to prove intersectScissorBox.
        BContainer tall = new BContainer(GroupLayout.makeVert(GroupLayout.TOP));
        ((GroupLayout)tall.getLayoutManager()).setGap(4);
        tall.setStyleClass("scrollcontent");
        for (int ii = 1; ii <= 20; ii++) {
            tall.add(new BLabel("scroll row " + ii, "scrolllabel"));
        }
        BScrollPane scroll = new BScrollPane(tall);
        scroll.setPreferredSize(320, 140);
        window.add(scroll);

        return window;
    }

    @Override
    public void simpleUpdate (float tpf)
    {
        if (_root != null && _window != null && !_packed) {
            // pack and center once the display dimensions are live.
            _window.pack();
            // center manually: BWindow.center() reads the backend's display size, which is
            // only populated inside beginFrame (render thread) -- it is still 0 here in update.
            int w = settings.getWidth(), h = settings.getHeight();
            _window.setLocation((w - _window.getWidth()) / 2, (h - _window.getHeight()) / 2);
            _window.validate();
            _packed = true;
            System.err.println("BUI window bounds: x=" + _window.getX() + " y=" + _window.getY() +
                " w=" + _window.getWidth() + " h=" + _window.getHeight() +
                " components=" + _window.getComponentCount());
        }
        if (_packed) {
            _frames++;
            // give the scene a few frames to settle, then grab a framebuffer screenshot.
            if (_frames == SHOT_FRAME) {
                _shotter.takeScreenshot();
            }
            // exit on a schedule so the run is self-terminating (also gives an X11 grab a
            // window to fire in, and lets the screenshot flush to disk).
            if (_frames >= EXIT_AFTER_FRAMES) {
                stop();
            }
        }
    }

    /**
     * Renders BUI during the GUI viewport's queue flush. At {@code postQueue} we are drawing
     * into the GUI viewport's framebuffer; we switch the render manager into jME3's built-in
     * 2D ORTHO overlay mode ({@code setCamera(cam, true)}, 1 unit == 1 pixel, origin
     * bottom-left -- exactly BUI's coordinate space) for the BUI draws, then restore the
     * camera so the GUI bucket flush that follows is undisturbed.
     */
    protected class BuiProcessor implements SceneProcessor
    {
        public void initialize (RenderManager rm, ViewPort vp) {
            _rm = rm;
            _initialized = true;
        }
        public boolean isInitialized () {
            return _initialized;
        }
        public void postQueue (RenderQueue rq) {
            if (_root == null || _rm == null) {
                return;
            }
            int w = settings.getWidth(), h = settings.getHeight();
            // RenderManager.setCamera(cam, true) installs jME3's built-in pixel-space 2D ORTHO
            // overlay matrix (origin bottom-left, 1 unit == 1 pixel) sized to the camera's
            // width/height -- exactly BUI's coordinate space. This is the same matrix the GUI
            // bucket uses; the stock gui-viewport camera's own (perspective-ish) frustum is
            // irrelevant under this overlay mode, which is why the earlier draws were clipped.
            _rm.setCamera(guiViewPort.getCamera(), true);
            _backend.beginFrame(_rm, w, h);
            try {
                // null token: the jME3 backend renders through its own RenderManager and
                // ignores BUI's pass-through Renderer argument.
                _root.renderWindows(null);
            } finally {
                _backend.endFrame();
                // restore non-overlay camera so the GUI bucket flush after us is undisturbed.
                _rm.setCamera(guiViewPort.getCamera(), false);
            }
        }
        public void preFrame (float tpf) {}
        public void postFrame (FrameBuffer out) {}
        public void reshape (ViewPort vp, int w, int h) {}
        public void cleanup () { _initialized = false; }
        public void setProfiler (AppProfiler profiler) {}

        protected RenderManager _rm;
        protected boolean _initialized;
    }

    protected Jme3RenderBackend _backend;
    protected BuiRootNode _root;
    protected BWindow _window;
    protected ScreenshotAppState _shotter;
    protected boolean _packed;
    protected int _frames;

    protected static final int WIDTH = 1024, HEIGHT = 768;
    protected static final int SHOT_FRAME = 30;          // grab once the scene has settled
    protected static final int EXIT_AFTER_FRAMES = 480;  // ~8s @ 60fps, self-terminating
}
