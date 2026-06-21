//
// $Id$

package com.threerings.bang.util;

import java.util.List;

import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 * Renders scene content to an offscreen texture.
 *
 * <p>jME3 cutover (Phase 2, cluster 6 — migration map §2.5 {@code TextureRenderer} REBUILD): the
 * fork implementation rendered into the back buffer and copied the result with raw
 * {@code GL11.glCopyTexImage2D}, poking the gdx renderer's {@code TextureStateRecord}. jME3
 * render-to-texture is viewport-based, so this is rebuilt on a {@link FrameBuffer} + offscreen
 * {@link ViewPort}: {@link #setupTexture} attaches the supplied {@link Texture2D} as the
 * framebuffer's color target, and {@link #render} renders the given scene content into it through
 * {@link RenderManager#renderViewPort}.
 *
 * <p>The public surface (ctor, {@code setBackgroundColor}, {@code getCamera}/{@code setCamera},
 * {@code setupTexture}, {@code render}, {@code cleanup}) is preserved so the RTT consumers
 * (avatar/unit portraits, {@code effect.RepairViz}) port by retargeting their method bodies, not
 * their call shape. The actual offscreen render must run on the GL thread.
 *
 * <p>Phase-4 note (fidelity): the fork variant rendered into whatever back-buffer state was current
 * and copied a sub-rect; this FBO version renders a clean offscreen pass. Visual parity of the
 * RTT-based glow/portrait effects is a Phase-4 visual-review item.
 */
public class BackTextureRenderer
{
    public BackTextureRenderer (BasicContext ctx, int width, int height)
    {
        _ctx = ctx;
        _width = width;
        _height = height;
        _camera = new Camera(width, height);
        _fbo = new FrameBuffer(width, height, 1);
        _fbo.setDepthBuffer(Image.Format.Depth);
        _viewport = new ViewPort("BackTextureRenderer", _camera);
        _viewport.setClearFlags(true, true, true);
        _viewport.setOutputFrameBuffer(_fbo);
        _viewport.setBackgroundColor(_bgcolor);
    }

    /** Returns the offscreen camera used for the RTT pass. */
    public Camera getCamera ()
    {
        return _camera;
    }

    /** Sets the offscreen camera used for the RTT pass. */
    public void setCamera (Camera camera)
    {
        _camera = camera;
    }

    /** Returns the clear color used for the RTT pass. */
    public ColorRGBA getBackgroundColor ()
    {
        return _bgcolor;
    }

    /** Sets the clear color used for the RTT pass. */
    public void setBackgroundColor (ColorRGBA c)
    {
        _bgcolor.set(c);
        _viewport.setBackgroundColor(_bgcolor);
    }

    /** Returns the offscreen target dimensions. */
    public int getWidth ()
    {
        return _width;
    }

    /** Returns the offscreen target dimensions. */
    public int getHeight ()
    {
        return _height;
    }

    /**
     * Binds the supplied texture as the framebuffer's color target. The texture is (re)sized to the
     * renderer's dimensions and rendered into by subsequent {@link #render} calls.
     */
    public void setupTexture (Texture2D tex)
    {
        tex.setImage(new Image(Image.Format.RGBA8, _width, _height,
            (java.nio.ByteBuffer)null, com.jme3.texture.image.ColorSpace.sRGB));
        _fbo.setColorTexture(tex);
    }

    /**
     * Renders the supplied spatial into the offscreen framebuffer (and thus into the bound color
     * texture). Must be called on the GL thread, outside the normal scene render.
     */
    public void render (Spatial scene)
    {
        RenderManager rm = _ctx.getRenderManager();
        _viewport.clearScenes();
        _viewport.attachScene(scene);
        scene.updateGeometricState();
        rm.renderViewPort(_viewport, 0f);
    }

    /**
     * Renders the supplied scene content into the offscreen framebuffer.
     */
    public void render (List<? extends Spatial> scenes)
    {
        RenderManager rm = _ctx.getRenderManager();
        _viewport.clearScenes();
        for (Spatial scene : scenes) {
            _viewport.attachScene(scene);
            scene.updateGeometricState();
        }
        rm.renderViewPort(_viewport, 0f);
    }

    /**
     * Releases offscreen resources. jME3 reclaims the framebuffer and its attachments when this
     * object is no longer referenced; this detaches the scenes so they may be collected.
     */
    public void cleanup ()
    {
        _viewport.clearScenes();
    }

    /** The application context. */
    protected BasicContext _ctx;

    /** The dimensions of the target texture. */
    protected int _width, _height;

    /** The current clear color. */
    protected ColorRGBA _bgcolor = new ColorRGBA(0f, 0f, 0f, 0f);

    /** The offscreen camera. */
    protected Camera _camera;

    /** The offscreen framebuffer and viewport. */
    protected FrameBuffer _fbo;
    protected ViewPort _viewport;
}
