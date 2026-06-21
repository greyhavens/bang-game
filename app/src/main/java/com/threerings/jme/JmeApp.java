//
// $Id$
//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2010 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/nenya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.jme;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.samskivert.util.RunQueue;
import com.samskivert.util.StringUtil;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;

import com.jmex.bui.BRootNode;
import com.jmex.bui.Jme3RootNode;
import com.jmex.bui.backend.BackendProvider;
import com.jmex.bui.backend.Jme3RenderBackend;

import com.threerings.jme.camera.CameraHandler;
import com.threerings.jme.camera.GodViewHandler;

/**
 * Defines a basic application framework providing integration with the Presents networking system
 * and a jME3 scene-graph host.
 *
 * <p>jME3 cutover (Phase 3 — the atomic host flip): this is now a
 * {@link com.jme3.app.SimpleApplication} on the jME3/LWJGL3 context. It owns the window, the GL
 * context, the main loop and input. The fork+gdx host (gdx {@code LwjglApplication} +
 * {@code ApplicationListener}) and the fork {@code DisplaySystem}/{@code Renderer} are gone. The
 * loop:
 * <ul>
 *   <li>{@link #simpleInitApp} installs the engine services ({@link #init}), the BUI render
 *       backend ({@link Jme3RenderBackend} via {@link BackendProvider#set}) and a
 *       {@link SceneProcessor} on the GUI viewport that brackets BUI's window traversal with
 *       {@code beginFrame}/{@code endFrame} (the proven {@code tools/jme3-host} pattern), wires
 *       the {@link Jme3RootNode} input path to the {@code InputManager}, and calls the
 *       subclass {@link #create} hook;</li>
 *   <li>{@link #simpleUpdate} drains posted runnables (Presents run queue) on the render thread,
 *       drives the BUI root state, and calls the subclass {@link #update} hook;</li>
 *   <li>{@link #destroy} calls the subclass {@link #cleanup} hook.</li>
 * </ul>
 */
public class JmeApp extends SimpleApplication
    implements RunQueue
{
    public JmeApp ()
    {
        // we manage our own scene; SimpleApplication's default state set (fly-cam, stats, debug
        // keys) is suppressed in simpleInitApp.
    }

    /**
     * Returns the Bang-side context (our {@link JmeContext}, distinct from the jME3 system
     * {@code com.jme3.system.JmeContext} that {@code Application.getContext()} returns).
     */
    public JmeContext getJmeContext ()
    {
        return _context;
    }

    /**
     * Installs the jME3 services this host exposes. Called from {@link #simpleInitApp} with the
     * engine-owned services.
     */
    public void init (AssetManager assetManager, RenderManager renderManager, Camera camera)
    {
        _assetManager = assetManager;
        _renderManager = renderManager;
        _camera = camera;
        _dispatchThread = Thread.currentThread();

        initRoot();
        initInput();
        initInterface();
    }

    @Override // from SimpleApplication
    public void simpleInitApp ()
    {
        // suppress SimpleApplication's defaults: we drive our own camera + UI.
        if (flyCam != null) {
            flyCam.setEnabled(false);
        }
        if (stateManager.getState(com.jme3.app.StatsAppState.class) != null) {
            stateManager.detach(stateManager.getState(com.jme3.app.StatsAppState.class));
        }
        if (stateManager.getState(com.jme3.app.DebugKeysAppState.class) != null) {
            stateManager.detach(stateManager.getState(com.jme3.app.DebugKeysAppState.class));
        }
        // SimpleApplication.initialize() registers a default ESCAPE -> stop() exit mapping; drop it
        // so Esc no longer instantly aborts the game (the game handles Esc itself: back/dismiss).
        if (inputManager.hasMapping(INPUT_MAPPING_EXIT)) {
            inputManager.deleteMapping(INPUT_MAPPING_EXIT);
        }
        inputManager.setCursorVisible(true);

        // register an asset locator rooted at the dev resource tree (the staged rsrc dir the
        // bin/ scripts point -Dresource_dir at) so the client can load baked model.j3o, textures
        // and materials by their rsrc-relative paths. jME3's stock Common/ MatDefs ship inside
        // jme3-core on the classpath (the default ClasspathLocator already finds them).
        String rsrcDir = System.getProperty("resource_dir");
        if (rsrcDir != null) {
            java.io.File root = new java.io.File(rsrcDir);
            if (root.isDirectory()) {
                assetManager.registerLocator(
                    root.getAbsolutePath(), com.jme3.asset.plugins.FileLocator.class);
            }
        }

        // make the main viewport clear to black (host owns the viewport background).
        viewPort.setBackgroundColor(ColorRGBA.Black);

        // Order the main viewport's transparent bucket by an explicit per-geometry render-order key
        // (then back-to-front as usual). The terrain renderer tags its alpha-blended splat overlay
        // layers so the whole terrain surface draws before the transparent move/attack highlights,
        // restoring the fork's "terrain before highlights" invariant (BUG-31); untagged transparent
        // geometry (water, particles, effects, UI) keeps its stock relative ordering.
        // (-Dbang.debug.noRenderOrder=true disables it, for A/B diagnosis of transparent-sort issues)
        if (!Boolean.getBoolean("bang.debug.noRenderOrder")) {
            viewPort.getQueue().setGeometryComparator(
                RenderQueue.Bucket.Transparent, new com.threerings.jme.util.RenderOrderComparator());
        }

        // install the BUI render backend BEFORE building any widget (BImage chooses its backing
        // from BackendProvider.get() at construction time).
        _backend = new Jme3RenderBackend(assetManager);
        BackendProvider.set(_backend);

        // install our services + build the scene graph + UI root.
        init(assetManager, renderManager, cam);

        // Establish our standard board perspective frustum on the main 3D camera (the jME3 default
        // camera ships a near:far that far-clips the full-size sky cloud disk -- see FRUSTUM_FAR).
        // The GLFW framebuffer-resize path (RenderManager.notifyReshape -> Camera.resize) preserves
        // near/far, so this far plane survives window resizes; OptionsView re-applies the same
        // FRUSTUM_NEAR/FRUSTUM_FAR on an explicit display-mode change.
        setPerspectiveFrustum(cam.getWidth(), cam.getHeight());

        // attach our scene-graph root to the main viewport's scene.
        rootNode.attachChild(_root);

        // some basic lighting so 3D content is not black (board view adds its own per-board
        // lights; this is a fallback for menu/town scenes).
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -1f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);
        AmbientLight amb = new AmbientLight();
        amb.setColor(ColorRGBA.White.mult(0.4f));
        rootNode.addLight(amb);

        // wire the camera-control input mappings now that we own the InputManager.
        if (_inputHandler != null) {
            _inputHandler.registerWith(inputManager);
        }

        // wire BUI input: attach the Jme3RootNode's RawInputListener to the InputManager, and hand
        // it the RenderManager so embedded-3D widgets (BGeomView) can create per-component
        // viewports.
        if (_rnode instanceof Jme3RootNode) {
            ((Jme3RootNode)_rnode).registerWith(inputManager);
            ((Jme3RootNode)_rnode).setRenderManager(renderManager);
        }

        // hand BCursor the live InputManager so hardware cursors (BCursor.show) can install
        // through jME3/GLFW. Mirrors the fork's static org.lwjgl.input.Mouse.setNativeCursor seam.
        com.jmex.bui.BCursor.setInputManager(inputManager);

        // render BUI through a SceneProcessor on the GUI viewport (the tools/jme3-host pattern):
        // postQueue runs with the GUI viewport active; we switch into the 2D ortho overlay and
        // bracket the window traversal with beginFrame/endFrame.
        guiViewPort.addProcessor(new BuiProcessor());

        // let the subclass finish initializing (creates the BangClient, etc.).
        create();
    }

    @Override // from com.jme3.app.LegacyApplication
    public void reshape (int width, int height)
    {
        // jME3 calls this on the render thread whenever the framebuffer changes size: a windowed
        // resize (dragging the window edge), a fullscreen toggle, or an AppSettings restart()
        // (OptionsView.switchDisplayMode). super.reshape -> RenderManager.notifyReshape re-fits
        // every viewport camera (the 3D board camera AND the guiViewPort/BUI camera) to the new
        // size; do that first so the cameras and the perspective frustum below are correct.
        super.reshape(width, height);
        if (width <= 0 || height <= 0) {
            return;
        }

        // re-apply our standard board perspective frustum for the new aspect ratio (mirrors the
        // fork's JmeApp.resize, which called setPerspectiveFrustum after the renderer reinit;
        // Camera.resize already preserves near/far, but this keeps the 45-degree fov + FRUSTUM_NEAR/
        // FRUSTUM_FAR authoritative and matches OptionsView's explicit re-fit on a mode change).
        setPerspectiveFrustum(width, height);

        // propagate the new size to BUI: the fork relied on the DisplaySystem recording a single
        // display size that BUI windows/popups/tooltips lay out from. Our backend records that size
        // per-frame from the GUI camera in beginFrame; push the new size in now so layout that runs
        // synchronously below (and on this tick) reads the new size rather than the previous frame's.
        if (_backend != null) {
            _backend.setDisplaySize(width, height);
        }
        // tell the BUI root the display resized (invalidates existing windows so they re-layout /
        // re-anchor against the new size on the next tick).
        if (_rnode != null) {
            _rnode.displayResized(width, height);
        }
        // notify any host resize listeners (BangClient re-bounds the active full-screen place view
        // -- TownView/BangView fill the screen; ShopView/LogonView re-center -- the way the fork's
        // OptionsView re-bounded its _parent after recreateWindow).
        for (ResizeListener l : _resizeListeners) {
            try {
                l.displayResized(width, height);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    /**
     * Registers a listener notified (on the render thread) whenever the host display is resized,
     * after the viewport cameras and the BUI backend size have been updated. The active view
     * controller uses this to re-bound full-screen views to the new size.
     */
    public void addResizeListener (ResizeListener listener)
    {
        if (!_resizeListeners.contains(listener)) {
            _resizeListeners.add(listener);
        }
    }

    /**
     * Removes a previously registered resize listener.
     */
    public void removeResizeListener (ResizeListener listener)
    {
        _resizeListeners.remove(listener);
    }

    @Override // from SimpleApplication
    public void simpleUpdate (float tpf)
    {
        _frameTime = tpf;
        _frameRate = (tpf > 0) ? 1f / tpf : 0f;

        // drain runnables posted from other threads (Presents run queue) on the render thread.
        executeRunnables();

        // advance the active camera path (game-start board tour, swing/roll, pans). The fork's
        // main loop ticked this every frame; SimpleApplication.simpleUpdate must do the same or
        // scripted camera moves (CameraHandler.moveCamera) are set but never animate.
        if (_camhand != null) {
            _camhand.update(tpf);
        }

        // drive per-frame BUI logic (tooltips, geom views, key repeat).
        if (_rnode != null) {
            _rnode.updateRootState(tpf);
        }

        // subclass per-frame work (sound stream updates, etc.).
        update((long)(tpf * 1000));
    }

    @Override // from SimpleApplication
    public void destroy ()
    {
        try {
            cleanup();
        } finally {
            super.destroy();
        }
    }

    /**
     * Subclass hook: called once at the end of {@link #simpleInitApp}, on the render thread, with
     * all services installed. Builds the client.
     */
    public void create ()
    {
    }

    /**
     * Subclass hook: called once per frame from {@link #simpleUpdate} on the render thread.
     */
    protected void update (long frameTick)
    {
    }

    /**
     * Subclass hook: called from {@link #destroy} as the app tears down.
     */
    protected void cleanup ()
    {
    }

    /**
     * (Re)applies our standard perspective frustum to the camera for the given viewport size.
     */
    public void setPerspectiveFrustum (int width, int height)
    {
        if (_camera != null && width > 0 && height > 0) {
            _camera.setFrustumPerspective(45.0f, width / (float)height, FRUSTUM_NEAR, FRUSTUM_FAR);
        }
    }

    /**
     * Returns the frames per second averaged over recent frames.
     */
    public float getRecentFrameRate ()
    {
        return _frameRate;
    }

    /**
     * Sets the main 3D ViewPort's clear/background color (the host owns the viewport; views that
     * used to set the fork display background call through here).
     */
    public void setViewportBackground (ColorRGBA color)
    {
        if (viewPort != null) {
            viewPort.setBackgroundColor(color);
        }
    }

    /**
     * Sets the window title (replaces the fork {@code Display.setTitle}).
     */
    public void setWindowTitle (String title)
    {
        com.jme3.system.JmeContext ctx = getContext();
        if (ctx != null) {
            ctx.setTitle(title);
        }
    }

    /**
     * Installs the application/window icon(s) on the live LWJGL3/GLFW window (shown in the window
     * corner and the OS task bar). Replaces the fork's LWJGL2 {@code Display.setIcon(ByteBuffer[])}:
     * the fork handed each icon's raw RGBA bytes to LWJGL2; here we convert each {@link BufferedImage}
     * to a {@link org.lwjgl.glfw.GLFWImage} (tightly-packed RGBA, top-left origin -- the layout GLFW
     * wants, mirroring jME3's own {@code LwjglWindow.setWindowIcon}) and call
     * {@code glfwSetWindowIcon}. GLFW copies the pixels internally, so the native buffers are freed
     * (try-with-resources) once the call returns.
     *
     * <p>Must be called on the render thread once the GLFW window exists (i.e. from {@link #create}
     * onward); {@code glfwGetCurrentContext} returns this app's window handle there. A no-op (logged
     * only) if no icons are supplied or the window handle is unavailable.
     */
    public void setWindowIcons (java.awt.image.BufferedImage[] icons)
    {
        if (icons == null || icons.length == 0) {
            return;
        }
        long window = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
        if (window == 0L) {
            System.err.println("JmeApp.setWindowIcons: no current GLFW window; icons not set.");
            return;
        }
        try (org.lwjgl.glfw.GLFWImage.Buffer iconSet =
                 org.lwjgl.glfw.GLFWImage.malloc(icons.length)) {
            // keep references to the per-image pixel buffers alive until the glfw call returns.
            java.nio.ByteBuffer[] pixels = new java.nio.ByteBuffer[icons.length];
            for (int ii = 0; ii < icons.length; ii++) {
                java.awt.image.BufferedImage img = icons[ii];
                int w = img.getWidth(), h = img.getHeight();
                java.nio.ByteBuffer buf = org.lwjgl.BufferUtils.createByteBuffer(w * h * 4);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int argb = img.getRGB(x, y);
                        buf.put((byte)((argb >> 16) & 0xFF)); // R
                        buf.put((byte)((argb >> 8) & 0xFF));  // G
                        buf.put((byte)(argb & 0xFF));         // B
                        buf.put((byte)((argb >> 24) & 0xFF)); // A
                    }
                }
                buf.flip();
                pixels[ii] = buf;
                org.lwjgl.glfw.GLFWImage gimg = org.lwjgl.glfw.GLFWImage.malloc();
                gimg.set(w, h, buf);
                iconSet.put(ii, gimg);
                gimg.free();
            }
            org.lwjgl.glfw.GLFW.glfwSetWindowIcon(window, iconSet);
        } catch (Throwable t) {
            System.err.println("JmeApp.setWindowIcons: failed to set window icon: " + t);
        }
    }

    /**
     * Returns true if the live LWJGL3/GLFW context is currently in fullscreen mode (reads the
     * settings the context applied, not a stored preference). Replaces the fork's
     * {@code DisplaySystem.isFullScreen()}.
     */
    public boolean isFullscreen ()
    {
        AppSettings s = (settings != null) ? settings : getContext().getSettings();
        return s != null && s.isFullscreen();
    }

    /**
     * Returns the current window width (logical pixels) of the live context, or 0 if unavailable.
     */
    public int getDisplayWidth ()
    {
        return (_camera != null) ? _camera.getWidth() : 0;
    }

    /**
     * Returns the current window height (logical pixels) of the live context, or 0 if unavailable.
     */
    public int getDisplayHeight ()
    {
        return (_camera != null) ? _camera.getHeight() : 0;
    }

    /**
     * Returns the native (desktop) resolution of the primary monitor as <code>{width, height,
     * refreshHz}</code>, read from the LWJGL3/GLFW context's monitor list. Returns
     * <code>null</code> if the context cannot supply it (e.g. headless). This replaces the fork's
     * LWJGL2 {@code Display.getDisplayMode()} for picking a sane fullscreen target — the jME3
     * {@code DisplayInfo} exposes only the monitor's current mode, which is exactly the mode we
     * want for desktop fullscreen.
     */
    public int[] getPrimaryMonitorMode ()
    {
        try {
            com.jme3.system.JmeContext ctx = getContext();
            if (ctx == null) {
                return null;
            }
            com.jme3.system.Displays displays = ctx.getDisplays();
            if (displays == null || displays.size() == 0) {
                return null;
            }
            com.jme3.system.DisplayInfo info = displays.get(ctx.getPrimaryDisplay());
            if (info == null) {
                return null;
            }
            return new int[] { info.getWidth(), info.getHeight(), info.getRate() };
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Switches the live LWJGL3/GLFW window to the requested display mode by mutating our
     * {@link AppSettings} and restarting the context (the jME3 way; the fork called
     * {@code DisplaySystem.recreateWindow}). Safe to call from any thread — the work is posted to
     * the render thread. A windowed switch resizes the existing window; a fullscreen switch
     * recreates it against the primary monitor.
     *
     * @param width  desired window/framebuffer width.
     * @param height desired window/framebuffer height.
     * @param bpp    bits per pixel (16/24/32); &lt;= 0 leaves the current value.
     * @param freq   refresh rate in Hz; &lt;= 0 leaves the current value.
     * @param fullscreen whether to go fullscreen.
     */
    public void switchDisplayMode (
        final int width, final int height, final int bpp, final int freq, final boolean fullscreen)
    {
        postRunnable(new Runnable() {
            public void run () {
                AppSettings s = settings;
                if (s == null) {
                    return;
                }
                s.setResizable(true);
                s.setFullscreen(fullscreen);
                if (bpp > 0) {
                    s.setBitsPerPixel(bpp);
                }
                if (freq > 0) {
                    s.setFrequency(freq);
                }
                // setResolution/setWindowSize keep the framebuffer and the logical window size in
                // step (matters for HiDPI); both set so the GLFW window comes up at the size.
                s.setWindowSize(width, height);
                s.setResolution(width, height);
                // applies the mutated settings to the context and triggers a GLFW window
                // recreate; the framebuffer-resize callback re-fits the viewport cameras.
                restart();
            }
        });
    }

    /**
     * Returns a GL driver identity string (vendor / renderer / version) for the perf report
     * (replaces the fork's LWJGL2 {@code GL11.glGetString} reads). Best-effort; must be called on
     * the render thread.
     */
    public String getGLRendererString ()
    {
        try {
            String vendor = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR);
            String renderer = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
            String version = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION);
            return vendor + " / " + renderer + " / " + version;
        } catch (Throwable t) {
            return "unknown";
        }
    }

    /**
     * Requests a framebuffer screenshot be written to the local data dir on the next frame
     * (replaces the fork {@code Renderer.takeScreenShot}). Uses a jME3 {@code ScreenshotAppState}.
     */
    public void takeScreenshot (String dir, String filename)
    {
        com.jme3.app.state.ScreenshotAppState shotter =
            stateManager.getState(com.jme3.app.state.ScreenshotAppState.class);
        if (shotter == null) {
            shotter = new com.jme3.app.state.ScreenshotAppState(dir + java.io.File.separator);
            stateManager.attach(shotter);
        }
        shotter.takeScreenshot();
    }

    // from interface RunQueue
    public void postRunnable (Runnable r)
    {
        // queued here; drained on the jME3 render thread in simpleUpdate (replacing gdx's
        // postRunnable). If we are already on the render thread we could run immediately, but
        // deferring to the next frame matches the old semantics.
        _runnables.add(r);
    }

    /**
     * Drains and runs any queued runnables. Called once per frame on the render thread.
     */
    public void executeRunnables ()
    {
        Runnable r;
        while ((r = _runnables.poll()) != null) {
            try {
                r.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    // from interface RunQueue
    public boolean isDispatchThread ()
    {
        return Thread.currentThread() == _dispatchThread;
    }

    // from interface RunQueue
    public boolean isRunning ()
    {
        return !_finished;
    }

    @Override // from SimpleApplication / Application
    public void stop ()
    {
        _finished = true;
        super.stop();
    }

    /**
     * Sets up a main input controller to handle the camera and deal with global user input.
     */
    protected void initInput ()
    {
        _camhand = createCameraHandler(_camera);
        _inputHandler = createInputHandler(_camhand);
    }

    /**
     * Creates the camera handler which provides various camera manipulation functionality.
     */
    protected CameraHandler createCameraHandler (Camera camera)
    {
        return new CameraHandler(camera);
    }

    /**
     * Creates the input handler (replacing the fork's polled {@code InputHandler}). Subclasses
     * override to install a game- or editor-specific handler.
     */
    protected GodViewHandler createInputHandler (CameraHandler camhand)
    {
        return new GodViewHandler(camhand);
    }

    /**
     * Creates our root node and the geometry sub-node.
     */
    protected void initRoot ()
    {
        _root = new Node("Root");
        _geom = new Node("Geometry");
        _root.attachChild(_geom);
    }

    /**
     * Initializes our user interface bits.
     */
    protected void initInterface ()
    {
        _iface = new Node("Interface");
        _root.attachChild(_iface);
        _rnode = createRootNode();
    }

    /**
     * Allows a customized BUI root node to be created. The default is the jME3 input-fed
     * {@link Jme3RootNode}.
     */
    protected BRootNode createRootNode ()
    {
        return new Jme3RootNode();
    }

    /**
     * Prepends the necessary bits onto the supplied path to properly locate it in our
     * configuration directory.
     */
    protected String getConfigPath (String file)
    {
        String cfgdir = ".narya";
        String home = System.getProperty("user.home");
        if (!StringUtil.isBlank(home)) {
            cfgdir = home + File.separator + cfgdir;
        }
        File dir = new File(cfgdir);
        if (!dir.exists()) {
            dir.mkdir();
        }
        return cfgdir + File.separator + file;
    }

    /**
     * Notified when the host display (framebuffer) is resized. Replaces the fork's implicit
     * reliance on {@code DisplaySystem} recording the size plus {@code OptionsView} re-bounding the
     * active view: registered listeners (the game client) re-bound their full-screen views.
     */
    public interface ResizeListener
    {
        /** Called on the render thread once the viewport cameras and BUI backend size have been
         * updated to the new {@code width}x{@code height} (pixels). */
        void displayResized (int width, int height);
    }

    /** Our {@link JmeContext} implementation, reading the host's services. */
    protected class ContextImpl implements JmeContext
    {
        public AssetManager getAssetManager () {
            return _assetManager;
        }
        public RenderManager getRenderManager () {
            return _renderManager;
        }
        public Camera getCamera () {
            return _camera;
        }
        public CameraHandler getCameraHandler () {
            return _camhand;
        }
        public GodViewHandler getInputHandler () {
            return _inputHandler;
        }
        public Node getGeometry () {
            return _geom;
        }
        public Node getInterface () {
            return _iface;
        }
        public BRootNode getRootNode () {
            return _rnode;
        }
    }

    /**
     * Renders BUI during the GUI viewport's queue flush, using the proven tools/jme3-host
     * approach: switch into jME3's 2D ortho overlay (1 unit == 1 pixel, origin bottom-left =
     * BUI's coordinate space) and bracket the window traversal with the backend's
     * beginFrame/endFrame.
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
            if (_rnode == null || _rm == null) {
                return;
            }
            Camera gcam = guiViewPort.getCamera();
            int w = gcam.getWidth(), h = gcam.getHeight();
            _rm.setCamera(gcam, true);
            _backend.beginFrame(_rm, w, h);
            try {
                _rnode.renderWindows(null);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                _backend.endFrame();
                _rm.setCamera(gcam, false);
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

    /** Near plane of the standard board perspective frustum. */
    public static final float FRUSTUM_NEAR = 1f;

    /**
     * Far plane of the standard board perspective frustum. The fork's jME1 sky queue did not clip
     * the full-size cloud disk (SkyNode CLOUD_RADIUS=10000, CLOUD_HEIGHT=500) against the camera
     * far plane, but jME3 does; with a 10000 far the disk's visible rim (slant ~10012) was clipped
     * and the clouds disappeared. Push the far plane out so the full-size disk renders unclipped,
     * matching the fork look. Board geometry (terrain/units/props) lives within ~a few hundred
     * units, so the wider near:far ratio (1:100000) does not introduce visible depth z-fighting.
     */
    public static final float FRUSTUM_FAR = 100000f;

    protected Thread _dispatchThread;
    protected float _frameTime;
    protected float _frameRate;

    protected AssetManager _assetManager;
    protected RenderManager _renderManager;
    protected Camera _camera;
    protected CameraHandler _camhand;
    protected GodViewHandler _inputHandler;

    protected BRootNode _rnode;
    protected Jme3RenderBackend _backend;
    protected final JmeContext _context = new ContextImpl();

    protected boolean _updateEnabled = true, _renderEnabled = true;
    protected boolean _finished;

    protected Node _root, _geom, _iface;

    /** Runnables posted from other threads, drained on the render thread. */
    protected final ConcurrentLinkedQueue<Runnable> _runnables = new ConcurrentLinkedQueue<>();

    /** Host display-resize listeners (notified on the render thread from {@link #reshape}). */
    protected final java.util.List<ResizeListener> _resizeListeners =
        new java.util.concurrent.CopyOnWriteArrayList<>();
}
