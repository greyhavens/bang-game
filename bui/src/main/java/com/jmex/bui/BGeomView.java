//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

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
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;

import com.jmex.bui.util.Insets;
import com.jmex.bui.util.Rectangle;

/**
 * Displays 3D geometry (a {@link Spatial}) inside a normal user interface.
 *
 * <p> <b>jME3 cutover note:</b> the original widget drove its own fork {@code Camera} and called
 * the fork renderer's {@code unsetOrtho}/{@code draw}/{@code setOrtho} dance to render a 3D spatial
 * into the component's screen rect. jME3 has no immediate-mode draw hook; the equivalent is a
 * dedicated jME3 {@link ViewPort} whose camera viewport is set to the component's screen rect,
 * rendered each frame by the host's {@code RenderManager} and depth-cleared (but not color-cleared)
 * so the embedded 3D model composites over the UI without z-fighting it.
 *
 * <p> This widget creates that {@link ViewPort} when it is added to the hierarchy ({@link
 * #wasAdded}) and destroys it when removed ({@link #wasRemoved}), using the {@code RenderManager}
 * the concrete {@link Jme3RootNode} exposes. The viewport's camera viewport rect is refit to the
 * component's on-screen rectangle every frame in {@link #update} (which also steps the spatial's
 * controls/anim so it animates/rotates), so the model tracks the component as windows move/resize.
 */
public class BGeomView extends BComponent
{
    /**
     * Creates a view with no configured geometry. Geometry can be set later with {@link
     * #setGeometry}.
     */
    public BGeomView ()
    {
        this(null);
    }

    /**
     * Creates a view with the specified {@link Spatial} to be rendered.
     */
    public BGeomView (Spatial geom)
    {
        _geom = geom;
    }

    /**
     * Configures the spatial to be rendered by this view.
     */
    public void setGeometry (Spatial geom)
    {
        // if we have a live viewport, swap the attached scene over to the new spatial
        if (_viewport != null) {
            if (_geom != null) {
                _viewport.detachScene(_geom);
            }
            if (geom != null) {
                _viewport.attachScene(geom);
            }
        }
        _geom = geom;
    }

    /**
     * Returns the geometry rendered by this view.
     */
    public Spatial getGeometry ()
    {
        return _geom;
    }

    /**
     * Called every frame (while we're added to the view hierarchy) by the {@link BRootNode}.
     */
    public void update (float frameTime)
    {
        // NOTE: the spatial's logical/geometric state is deliberately NOT updated here. This
        // method runs during BUI's update pass, but the geometry can still be mutated afterwards
        // in the same frame (async model-load callbacks via executeRunnables, the model's own
        // rotation/anim controls), and our dedicated ViewPort renders later in render() -- so a
        // geometric update here would leave the spatial dirty at render time and jME3 would throw
        // "Scene graph is not properly updated for rendering" (checkCulling). Instead we update it
        // in the ViewPort's SceneProcessor.preFrame (see createViewport), which runs immediately
        // before this viewport renders, after all other per-frame scene mutation.

        // refit the viewport camera's screen rect to our on-screen rectangle (the component may
        // have moved or its window resized since the last frame). This touches only the camera,
        // not the scene graph, so it is safe to do here.
        refitViewport();
    }

    // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _root = getWindow().getRootNode();
        _root.registerGeomView(this);
        createViewport();
    }

    // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        destroyViewport();
        _root.unregisterGeomView(this);
        _root = null;
    }

    // documentation inherited
    protected void renderComponent (RenderManager renderer)
    {
        super.renderComponent(renderer);
        // The 3D render is performed by our dedicated jME3 ViewPort (see class javadoc and
        // createViewport); there is nothing to draw inline in BUI's 2D overlay pass.
    }

    /**
     * Returns the camera used to render our geometry into the component rect. The default builds a
     * perspective camera looking down -Z from a sensible default position (matching the fork's
     * {@code createCamera}). Subclasses ({@code UnitView}) that need a model-specific framing
     * override this to supply their own positioned camera; the viewport rect and frustum aspect are
     * managed here regardless.
     */
    protected Camera getViewCamera ()
    {
        // a 1x1 camera is fine; the real pixel size is established by refitViewport via the
        // fractional viewport rect, and the frustum aspect is set there too.
        Camera camera = new Camera(Math.max(1, getWidth()), Math.max(1, getHeight()));
        camera.setLocation(new Vector3f(0f, 0f, 25f));
        // look down -Z with +Y up (jME3 default frame), matching the fork's createCamera frame.
        camera.setAxes(new Vector3f(-1f, 0f, 0f), new Vector3f(0f, 1f, 0f),
                       new Vector3f(0f, 0f, -1f));
        return camera;
    }

    /**
     * Creates the dedicated jME3 {@link ViewPort} that renders our geometry into the component rect.
     * No-op if the host does not expose a {@code RenderManager} (e.g. a headless/test root node).
     */
    protected void createViewport ()
    {
        if (_viewport != null) {
            return;
        }
        RenderManager rm = (_root != null) ? _root.getRenderManager() : null;
        if (rm == null) {
            return;
        }

        _camera = getViewCamera();
        // Create a POST view, not a main view. jME3's RenderManager.render() draws
        // preViews -> mainViews -> postViews; BUI itself is drawn by a SceneProcessor on the GUI
        // viewport, which SimpleApplication registers as a *post* view at init -- so BUI (including
        // a UnitView's opaque panel background + frame chrome) renders during the post-view pass.
        // A main view here would render the model BEFORE that BUI pass, and the opaque panel would
        // then paint right over it (the "uniform teal, no model" symptom). Because the GUI viewport
        // is created first (at app init, long before any BGeomView exists) and post views render in
        // insertion order, this post view is appended AFTER the GUI viewport and therefore renders
        // AFTER BUI has drawn the panel -- so the model composites on top of the panel background,
        // inset within the surrounding frame chrome. This is the jME3 equivalent of the fork drawing
        // the model inline (unsetOrtho/draw/setOrtho) at the right z within the UI render pass.
        _viewport = rm.createPostView("BGeomView@" + hashCode(), _camera);
        // depth-clear only: keep the UI rendered behind us (no color clear) but start with a clean
        // depth buffer over our rect so the model doesn't z-fight whatever was drawn there. This
        // mirrors the fork's scissored glClear(GL_DEPTH_BUFFER_BIT).
        _viewport.setClearFlags(false, true, false);
        // update the spatial's controls + world transforms/bounds immediately before this viewport
        // renders (after all other per-frame scene mutation), so it is never dirty at render time.
        _viewport.addProcessor(new GeomUpdater());

        // a separate viewport has its own scene graph, so it needs its own lights for lit models
        // (the main scene's sun/ambient don't carry over). Use the same neutral fallback the host
        // installs on the main scene. Add them once per spatial (a remove/re-add cycle must not
        // stack duplicate lights).
        if (_geom != null) {
            if (_sun == null) {
                _sun = new DirectionalLight();
                _sun.setDirection(new Vector3f(-0.5f, -1f, -0.5f).normalizeLocal());
                _sun.setColor(ColorRGBA.White);
                _amb = new AmbientLight();
                _amb.setColor(ColorRGBA.White.mult(0.4f));
            }
            _geom.removeLight(_sun);
            _geom.removeLight(_amb);
            _geom.addLight(_sun);
            _geom.addLight(_amb);

            _viewport.attachScene(_geom);
        }

        refitViewport();
    }

    /**
     * Destroys our dedicated viewport (called from {@link #wasRemoved}), releasing it from the
     * host's {@code RenderManager} so nothing leaks.
     */
    protected void destroyViewport ()
    {
        if (_viewport == null) {
            return;
        }
        RenderManager rm = (_root != null) ? _root.getRenderManager() : null;
        if (rm != null) {
            // must match createViewport's createPostView (removeMainView would silently no-op and
            // leak the viewport, leaving a stale model rendered after the component is gone).
            rm.removePostView(_viewport);
        }
        if (_geom != null) {
            _viewport.detachScene(_geom);
        }
        _viewport = null;
        _camera = null;
    }

    /**
     * Sets our viewport camera's screen rect (as a fraction of the framebuffer) to our on-screen
     * rectangle, and keeps the frustum aspect in step with that rect. Called every frame.
     */
    protected void refitViewport ()
    {
        if (_viewport == null || _camera == null || _root == null) {
            return;
        }
        int swidth = _root.getDisplayWidth(), sheight = _root.getDisplayHeight();
        if (swidth <= 0 || sheight <= 0) {
            return;
        }

        Insets insets = getInsets();
        int ax = getAbsoluteX() + insets.left, ay = getAbsoluteY() + insets.bottom;
        int width = _width - insets.getHorizontal(), height = _height - insets.getVertical();
        if (width <= 0 || height <= 0) {
            return;
        }

        // Apply the SAME transform the BUI backend applies to our ancestor window's render. Inside a
        // scaled shop (e.g. Ranch UnitView) the window's pixels are scaled about its (getX,getY)
        // origin; our logical rect (ax,ay,width,height) is in unscaled BUI space, so map it through
        // the window scale to land on the correct on-screen (scaled) pane. For an unscaled window
        // (sx=sy=1) this reduces to exactly the previous values.
        BWindow win = getWindow();
        float sx = (win != null) ? win.getRenderScaleX() : 1f;
        float sy = (win != null) ? win.getRenderScaleY() : 1f;
        int winX = (win != null) ? win.getX() : 0;
        int winY = (win != null) ? win.getY() : 0;
        float vax = winX + (ax - winX) * sx;      // visual screen-space rect (BUI bottom-left origin)
        float vay = winY + (ay - winY) * sy;
        float vwidth = width * sx;
        float vheight = height * sy;

        if (ax == _cx && ay == _cy && width == _cwidth && height == _cheight &&
            swidth == _swidth && sheight == _sheight && sx == _cscaleX && sy == _cscaleY &&
            winX == _cwinX && winY == _cwinY) {
            return; // nothing changed
        }
        _cx = ax; _cy = ay; _cwidth = width; _cheight = height;
        _swidth = swidth; _sheight = sheight;
        _cscaleX = sx; _cscaleY = sy; _cwinX = winX; _cwinY = winY;

        // jME3 camera viewport is expressed as left/right/bottom/top fractions of the framebuffer
        // (origin bottom-left, matching BUI's coordinate space). Build the fractions from the
        // window-scaled visual rect so the viewport tracks the scaled pane.
        float left = vax / (float)swidth, right = (vax + vwidth) / (float)swidth;
        float bottom = vay / (float)sheight, top = (vay + vheight) / (float)sheight;
        // The camera's resolution MUST be the full framebuffer, not the component size: jME3 derives
        // the actual GL viewport from the fractional rect as fraction * camera.getWidth()/getHeight().
        // Sizing the camera to the component shrank the fractions onto a tiny bottom-left box. Resize
        // to the framebuffer first so the fractions below land on the correct on-screen region; the
        // perspective frustum then uses the (possibly stretched) visual rect's aspect (vwidth/vheight)
        // so the model stays undistorted and sits proportional/centered within that rect.
        _camera.resize(swidth, sheight, false);
        _camera.setViewPort(left, right, bottom, top);
        _camera.setFrustumPerspective(45.0f, vwidth / vheight, 1f, 1000f);
    }

    /**
     * Steps the rendered spatial's controls and refreshes its world transforms/bounds at the start
     * of our viewport's render (in {@code preFrame}), guaranteeing it is geometrically up to date
     * before jME3's {@code checkCulling} inspects it. This is the correct hook for a manually
     * created {@link ViewPort} whose scene jME3 does not auto-update like the main {@code rootNode}.
     */
    protected class GeomUpdater implements SceneProcessor
    {
        public void initialize (RenderManager rm, ViewPort vp) {
            _initialized = true;
        }
        public void reshape (ViewPort vp, int w, int h) {
            // nothing; our camera rect is driven by refitViewport
        }
        public boolean isInitialized () {
            return _initialized;
        }
        public void preFrame (float tpf) {
            if (_geom != null) {
                // jME3 splits the fork's single updateGeometricState(time, initiator) into a
                // logical (time-stepped controls/anim) and a geometric (transform/bound) update.
                _geom.updateLogicalState(tpf);
                _geom.updateGeometricState();
            }
        }
        public void postQueue (RenderQueue rq) {
            // nothing
        }
        public void postFrame (FrameBuffer out) {
            // nothing
        }
        public void cleanup () {
            _initialized = false;
        }
        public void setProfiler (AppProfiler profiler) {
            // nothing
        }
        protected boolean _initialized;
    }

    protected BRootNode _root;
    protected Spatial _geom;

    /** Our dedicated render viewport and its camera (null until added to a live host). */
    protected ViewPort _viewport;
    protected Camera _camera;

    /** Per-scene fallback lights (created once, reused across add/remove cycles). */
    protected DirectionalLight _sun;
    protected AmbientLight _amb;

    /** Cached screen rect / framebuffer size so refitViewport only reconfigures on change. */
    protected int _cx, _cy, _cwidth, _cheight, _swidth, _sheight;

    /** Cached ancestor-window render scale + origin, so a fullscreen toggle (which changes the
     * window's scale/position without changing our logical rect) also re-fits the viewport. */
    protected float _cscaleX = 1f, _cscaleY = 1f;
    protected int _cwinX, _cwinY;

    protected Rectangle _srect = new Rectangle();
}
