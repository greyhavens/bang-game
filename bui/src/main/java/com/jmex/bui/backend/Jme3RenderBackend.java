//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.backend;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;

import com.jmex.bui.BImage;
import com.jmex.bui.util.Rectangle;

/**
 * A {@link BRenderBackend} implementation on jMonkeyEngine 3 (3.9.0-stable).
 *
 * <p> This is Phase 5 step 3 groundwork (see {@code docs/jme3-bui-port.md}). It is compiled
 * against {@code jme3-core} via a {@code compileOnly} dependency, so jME3 (and its LWJGL3
 * transitive natives) never reach the shipped game's runtime classpath. The fork backend
 * ({@link JmeRenderBackend}) remains BUI's default; this backend is only installed by a host
 * jME3 application that supplies the jME3 runtime itself.
 *
 * <p> BUI keeps its immediate-mode painter model. Instead of fixed-function GL11 immediate
 * mode, each primitive is rendered by handing a transient {@link Geometry} (a {@link Quad}
 * mesh with an {@code Unshaded} material) to {@link RenderManager#renderGeometry}. BUI's
 * translate stack becomes an accumulated translation applied to the geometry's world
 * position; the scissor stack maps onto {@link Renderer#setClipRect}/{@code clearClipRect}.
 *
 * <p> The host must, every frame before BUI renders, call {@link #beginFrame} with the live
 * {@link RenderManager} and the current display dimensions, and {@link #endFrame} afterward.
 */
public class Jme3RenderBackend implements BRenderBackend
{
    /**
     * Creates the backend.
     *
     * @param assets the host application's asset manager, used to load the stock
     * {@code Unshaded.j3md} material definition for solid-color and textured primitives.
     */
    public Jme3RenderBackend (AssetManager assets)
    {
        _assets = assets;
        _solid = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        _solid.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        _solid.getAdditionalRenderState().setDepthTest(false);
        _solid.getAdditionalRenderState().setDepthWrite(false);
        // The GUI viewport's ortho camera looks toward +Z, while a jME3 Quad's front face
        // (CCW winding) has a +Z normal -- so the camera sees the quad's BACK face. With the
        // default Back cull mode that culls every BUI primitive (the symptom: geometry shows
        // in the render stats but nothing rasterizes). Disable face culling for flat UI quads.
        _solid.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
    }

    /**
     * Installs the per-frame render context. Call once per frame, on the render thread,
     * before any BUI component is rendered.
     */
    public void beginFrame (RenderManager rm, int displayWidth, int displayHeight)
    {
        _rm = rm;
        _displayWidth = displayWidth;
        _displayHeight = displayHeight;
        _tx = 0;
        _ty = 0;
    }

    /** Tears down the per-frame render context and clears any active scissor. */
    public void endFrame ()
    {
        if (_rm != null) {
            _rm.getRenderer().clearClipRect();
        }
        _rm = null;
    }

    /** Returns the asset manager handed to this backend (for {@link Jme3ImageBacking}). */
    public AssetManager getAssetManager ()
    {
        return _assets;
    }

    /** Returns the active render manager for the current frame, or null between frames. */
    public RenderManager getRenderManager ()
    {
        return _rm;
    }

    /** Returns the accumulated x translation of BUI's translate stack. */
    public int getTranslateX ()
    {
        return _tx;
    }

    /** Returns the accumulated y translation of BUI's translate stack. */
    public int getTranslateY ()
    {
        return _ty;
    }

    // documentation inherited from interface BRenderBackend
    public int getDisplayWidth ()
    {
        return _displayWidth;
    }

    // documentation inherited from interface BRenderBackend
    public int getDisplayHeight ()
    {
        return _displayHeight;
    }

    // documentation inherited from interface BRenderBackend
    public boolean isWindowActive ()
    {
        // jME3 hosts drive focus through their own context listener; assume active.
        return true;
    }

    // documentation inherited from interface BRenderBackend
    public void applyDefaultStates ()
    {
        // jME3 sets render state per-material/per-geometry; nothing global to reset.
    }

    // documentation inherited from interface BRenderBackend
    public void applyBlendState ()
    {
        // Alpha blending is configured on each primitive's material render state.
    }

    // documentation inherited from interface BRenderBackend
    public void translate (int x, int y)
    {
        _tx += x;
        _ty += y;
    }

    // documentation inherited from interface BRenderBackend
    public void fillRect (float x, float y, float width, float height,
                          float r, float g, float b, float a)
    {
        if (_rm == null) {
            return;
        }
        Geometry geom = quad(width, height);
        _solid.setColor("Color", new ColorRGBA(r, g, b, a));
        geom.setMaterial(_solid);
        drawAt(geom, x, y);
    }

    // documentation inherited from interface BRenderBackend
    public void drawRectOutline (float x, float y, float width, float height, float lineWidth,
                                 float r, float g, float b, float a)
    {
        // Approximate the outline with four filled edge rects (lineWidth thick), inset so the
        // lines lie within the rectangle bounds -- matches the fork's inset-by-half behavior
        // closely enough for the UI's 1-2px borders without needing a line mesh.
        float lw = Math.max(1f, lineWidth);
        fillRect(x, y, width, lw, r, g, b, a);                       // bottom
        fillRect(x, y + height - lw, width, lw, r, g, b, a);         // top
        fillRect(x, y, lw, height, r, g, b, a);                      // left
        fillRect(x + width - lw, y, lw, height, r, g, b, a);         // right
    }

    // documentation inherited from interface BRenderBackend
    public void drawLine (float x1, float y1, float x2, float y2,
                          float r, float g, float b, float a)
    {
        // BUI only draws axis-aligned 1px lines (text caret); render as a thin filled rect.
        float x = Math.min(x1, x2), y = Math.min(y1, y2);
        float w = Math.max(1f, Math.abs(x2 - x1)), h = Math.max(1f, Math.abs(y2 - y1));
        fillRect(x, y, w, h, r, g, b, a);
    }

    // documentation inherited from interface BRenderBackend
    public boolean intersectScissorBox (Rectangle store, int x, int y, int width, int height)
    {
        boolean enabled = _scissored;
        if (enabled) {
            store.set(_sx, _sy, _sw, _sh);
            int x1 = Math.max(x, store.x), y1 = Math.max(y, store.y),
                x2 = Math.min(x + width, store.x + store.width),
                y2 = Math.min(y + height, store.y + store.height);
            setClip(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
        } else {
            setClip(x, y, width, height);
        }
        return enabled;
    }

    // documentation inherited from interface BRenderBackend
    public void restoreScissorState (boolean enabled, Rectangle rect)
    {
        if (enabled) {
            setClip(rect.x, rect.y, rect.width, rect.height);
        } else {
            _scissored = false;
            if (_rm != null) {
                _rm.getRenderer().clearClipRect();
            }
        }
    }

    // documentation inherited from interface BRenderBackend
    public void clearZBuffer ()
    {
        // BUI runs depth-test-free; embedded 3D (BGeomView) gets its own jME3 ViewPort at the
        // final port and clears its own depth buffer there.
    }

    // documentation inherited from interface BRenderBackend
    public boolean supportsNonPowerOfTwo ()
    {
        // jME3 / modern GL always supports NPOT textures.
        return true;
    }

    // documentation inherited from interface BRenderBackend
    public BImageBacking createImageBacking (BImage image)
    {
        return new Jme3ImageBacking(this, image);
    }

    protected void setClip (int x, int y, int width, int height)
    {
        _scissored = true;
        _sx = x; _sy = y; _sw = width; _sh = height;
        if (_rm != null) {
            _rm.getRenderer().setClipRect(x, y, width, height);
        }
    }

    /**
     * Returns a reusable quad geometry sized to the supplied dimensions.
     *
     * <p>BUI repaints in immediate mode every frame, so this is called many times per frame. A
     * fresh {@code new Geometry(new Quad(...))} each call would allocate four {@link
     * com.jme3.scene.VertexBuffer}s whose GL upload registers them with jME3's {@code
     * NativeObjectManager}; those registrations are only released once the buffers are GC'd and
     * their GL handles deleted, which never happens for throwaway geometry, so the manager's ref
     * map grows without bound (a severe per-frame native-buffer leak). Instead we keep one cached
     * quad and resize it in place via {@link Quad#updateGeometry} -- {@code Mesh.setBuffer} reuses
     * the existing VertexBuffer objects (updateData), so no new native objects are ever registered
     * after the first frame.</p>
     */
    protected Geometry quad (float width, float height)
    {
        if (_quadMesh == null) {
            _quadMesh = new Quad(width, height);
            _quadGeom = new Geometry("bui-quad", _quadMesh);
        } else {
            _quadMesh.updateGeometry(width, height);
        }
        return _quadGeom;
    }

    /** Renders a geometry at the supplied BUI-space coordinates (translate stack applied). */
    protected void drawAt (Geometry geom, float x, float y)
    {
        geom.setLocalTranslation(_tx + x, _ty + y, 0);
        geom.updateGeometricState();
        _rm.renderGeometry(geom);
    }

    protected AssetManager _assets;
    protected Material _solid;

    /** Reused per-frame primitive quad (see {@link #quad}); resized in place to avoid leaking a
     * fresh mesh's VertexBuffers into jME3's NativeObjectManager every frame. */
    protected Quad _quadMesh;
    protected Geometry _quadGeom;

    protected RenderManager _rm;
    protected int _displayWidth, _displayHeight;
    protected int _tx, _ty;

    protected boolean _scissored;
    protected int _sx, _sy, _sw, _sh;
}
