//
// $Id$

package com.threerings.bang.util;

import java.util.ArrayList;

import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;

import com.jme.image.Texture;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.RenderContext;
import com.jme.renderer.Renderer;
import com.jme.renderer.TextureRenderer;
import com.jme.scene.Spatial;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.gdx.records.TextureStateRecord;
import com.jme.system.DisplaySystem;
import com.jme.util.geom.BufferUtils;

/**
 * A texture renderer that renders to and copies from the back buffer (which
 * means you mustn't call the {@link #render} methods during normal rendering).
 */
public class BackTextureRenderer
    implements TextureRenderer
{
    public BackTextureRenderer (BasicContext ctx, int width, int height)
    {
        _ctx = ctx;
        _width = width;
        _height = height;

        // createCamera updates, so be sure to call the old one
        _camera = ctx.getRenderer().createCamera(width, height);
        ctx.getCameraHandler().getCamera().update();
    }

    // documentation inherited from interface TextureRenderer
    public boolean isSupported ()
    {
        return true;
    }

    // documentation inherited from interface TextureRenderer
    public int getPBufferWidth ()
    {
        return _width;
    }

    // documentation inherited from interface TextureRenderer
    public int getPBufferHeight ()
    {
        return _height;
    }

    // documentation inherited from interface TextureRenderer
    public ColorRGBA getBackgroundColor ()
    {
        return _bgcolor;
    }

    // documentation inherited from interface TextureRenderer
    public void setBackgroundColor (ColorRGBA c)
    {
        _bgcolor = c;
    }

    // documentation inherited from interface TextureRenderer
    public Camera getCamera ()
    {
        return _camera;
    }

    // documentation inherited from interface TextureRenderer
    public void setCamera (Camera camera)
    {
        _camera = camera;
    }

    // documentation inherited from interface TextureRenderer
    public void updateCamera ()
    {
        _camera.update();
    }

    // documentation inherited from interface TextureRenderer
    public void setupTexture (Texture tex)
    {
        setupTexture(tex, _width, _height);
    }

    // documentation inherited from interface TextureRenderer
    public void setupTexture (Texture tex, int width, int height)
    {
        IntBuffer ibuf = BufferUtils.createIntBuffer(1);

        if (tex.getTextureId() != 0) {
            ibuf.put(tex.getTextureId());
            GL11.glDeleteTextures(ibuf);
            ibuf.clear();
        }

        // Create the texture
        GL11.glGenTextures(ibuf);
        tex.setTextureId(ibuf.get(0));

        copyToTexture(tex, width, height);
    }

    // documentation inherited from interface TextureRenderer
    public void render (ArrayList<?> spats, Texture tex)
    {
        // render to back buffer
        Renderer parentRenderer = _ctx.getRenderer();
        preDraw(parentRenderer);
        for (int ii = 0, nn = spats.size(); ii < nn; ii++) {
            ((Spatial)spats.get(ii)).onDraw(parentRenderer);
        }
        postDraw(parentRenderer);

        // copy back buffer to texture
        copyToTexture(tex, _width, _height);
    }

    // documentation inherited from interface TextureRenderer
    public void render (ArrayList<?> spats, Texture... texs)
    {
        // render to back buffer
        Renderer parentRenderer = _ctx.getRenderer();
        preDraw(parentRenderer);
        for (int ii = 0, nn = spats.size(); ii < nn; ii++) {
            ((Spatial)spats.get(ii)).onDraw(parentRenderer);
        }
        postDraw(parentRenderer);

        // copy back buffer to texture(s)
        for (Texture tex : texs) {
            copyToTexture(tex, _width, _height);
        }
    }

    // documentation inherited from interface TextureRenderer
    public void render (Spatial spat, Texture... texs)
    {
        // render to back buffer
        Renderer parentRenderer = _ctx.getRenderer();
        preDraw(parentRenderer);
        spat.onDraw(parentRenderer);
        postDraw(parentRenderer);

        // copy back buffer to texture(s)
        for (Texture tex : texs) {
            copyToTexture(tex, _width, _height);
        }
    }

    // documentation inherited from interface TextureRenderer
    public void copyToTexture (Texture tex, int width, int height)
    {
        TextureStateRecord record =
            (TextureStateRecord)_ctx.getDisplay().getCurrentContext().getStateRecord(
                RenderState.RS_TEXTURE);

        int texId = tex.getTextureId();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        record.units[record.currentUnit].boundTexture = texId;
        GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0,
            getInternalFormat(tex.getRTTSource()), 0, 0, width, height, 0);
    }

    // documentation inherited from interface TextureRenderer
    public void copyBufferToTexture (
        Texture tex, int width, int height, int buffer)
    {
        GL11.glReadBuffer(GL11.GL_BACK);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.getTextureId());
        GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, tex.getRTTSource(), 0,
            0, width, height, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    // documentation inherited from interface TextureRenderer
    public void forceCopy (boolean force)
    {
        // no-op
    }

    // documentation inherited from interface TextureRenderer
    public void cleanup ()
    {
        // leave it to the caller to unbind textures
    }

    /**
     * Sets the renderer up for drawing.
     */
    protected void preDraw (Renderer parentRenderer)
    {
        // grab non-rtt settings
        _oldCamera = parentRenderer.getCamera();
        _oldWidth = parentRenderer.getWidth();
        _oldHeight = parentRenderer.getHeight();
        _oldBackgroundColor = parentRenderer.getBackgroundColor();

        // swap to rtt settings
        parentRenderer.setCamera(_camera);
        parentRenderer.reinit(_width, _height);

        // Clear the states.
        applyDefaultStates();

        // do rtt scene render
        parentRenderer.setBackgroundColor(_bgcolor);
        parentRenderer.clearBuffers();
        parentRenderer.getQueue().swapBuckets();
    }

    /**
     * Returns the renderer to its normal state.
     */
    protected void postDraw (Renderer parentRenderer)
    {
        parentRenderer.renderQueue();

        // back to the non rtt settings
        parentRenderer.getQueue().swapBuckets();
        parentRenderer.setCamera(_oldCamera);
        parentRenderer.reinit(_oldWidth, _oldHeight);
        parentRenderer.setBackgroundColor(_oldBackgroundColor);

        // Clear the states again since we will be moving back to the old
        // location and don't want the states bleeding over causing things
        // *not* to be set when they should be.
        applyDefaultStates();
    }

    /**
     * Reverts to the default states.
     */
    protected static void applyDefaultStates ()
    {
        RenderContext ctx =
            DisplaySystem.getDisplaySystem().getCurrentContext();
        for (int ii = 0; ii < Renderer.defaultStateList.length; ii++) {
            if (Renderer.defaultStateList[ii] != null) {
                Renderer.defaultStateList[ii].apply();
            }
        }
        ctx.clearCurrentStates();
    }

    /**
     * Returns the internal format corresponding to the given render-to-texture
     * source.
     */
    protected static int getInternalFormat (int rttSource)
    {
        switch (rttSource) {
            case Texture.RTT_SOURCE_RGB: return GL11.GL_RGB;
            case Texture.RTT_SOURCE_ALPHA: return GL11.GL_ALPHA;
            case Texture.RTT_SOURCE_DEPTH: return GL11.GL_DEPTH_COMPONENT;
            case Texture.RTT_SOURCE_INTENSITY: return GL11.GL_INTENSITY;
            case Texture.RTT_SOURCE_LUMINANCE: return GL11.GL_LUMINANCE;
            case Texture.RTT_SOURCE_LUMINANCE_ALPHA:
                return GL11.GL_LUMINANCE_ALPHA;
            default: return GL11.GL_RGBA;
        }
    }

    /** The application context. */
    protected BasicContext _ctx;

    /** The dimensions of the target texture. */
    protected int _width, _height;

    /** The current background color. */
    protected ColorRGBA _bgcolor = new ColorRGBA();

    /** The current camera. */
    protected Camera _camera;

    /** The texture to which we shall render. */
    protected Texture _texture;

    /** State preserved between {@link #preDraw} and {@link #postDraw}. */
    protected int _oldWidth, _oldHeight;
    protected Camera _oldCamera;
    protected ColorRGBA _oldBackgroundColor;
}
