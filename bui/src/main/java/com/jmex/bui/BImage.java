//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.lwjgl.opengl.GLContext;

import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.AlphaState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;
import com.jme.util.TextureManager;

/**
 * Contains a texture, its dimensions and a texture state.
 */
public class BImage extends Quad
{
    /** An interface for pooling OpenGL textures. */
    public interface TexturePool
    {
        /**
         * Acquires textures from the pool for the state.
         */
        public void acquireTextures (TextureState tstate);

        /**
         * Releases the state's textures back into the pool.
         */
        public void releaseTextures (TextureState tstate);
    }

    /** An alpha state that blends the source plus one minus destination. */
    public static AlphaState blendState;

    /**
     * Configures the supplied spatial with transparency in the standard user interface sense which
     * is that transparent pixels show through to the background but non-transparent pixels are not
     * blended with what is behind them.
     */
    public static void makeTransparent (Spatial target)
    {
        target.setRenderState(blendState);
    }

    /**
     * Sets the texture pool from which to acquire and release OpenGL texture objects.
     * Applications can provide a pool in order to avoid the rapid creation and destruction of
     * OpenGL textures.  The default pool always creates new textures and deletes released ones.
     */
    public static void setTexturePool (TexturePool pool)
    {
        _texturePool = pool;
    }

    /**
     * Returns a reference to the configured texture pool.
     */
    public static TexturePool getTexturePool ()
    {
        return _texturePool;
    }

    /**
     * Creates an image from the supplied source URL.
     */
    public BImage (URL image)
        throws IOException
    {
        this(ImageIO.read(image), true);
    }

    /**
     * Creates an image from the supplied source AWT image.
     */
    public BImage (java.awt.Image image)
    {
        this(image, true);
    }

    /**
     * Creates an image from the supplied source AWT image.
     */
    public BImage (java.awt.Image image, boolean flip)
    {
        this(image.getWidth(null), image.getHeight(null));

        // expand the texture data to a power of two if necessary
        int twidth = _width, theight = _height;
        if (!_supportsNonPowerOfTwo) {
            twidth = nextPOT(twidth);
            theight = nextPOT(theight);
        }

        // render the image into a raster of the proper format
        boolean hasAlpha = TextureManager.hasAlpha(image);
        int type = hasAlpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;
        BufferedImage tex = new BufferedImage(twidth, theight, type);
        AffineTransform tx = null;
        if (flip) {
            tx = AffineTransform.getScaleInstance(1, -1);
            tx.translate(0, -_height);
        }
        Graphics2D gfx = (Graphics2D) tex.getGraphics();
        gfx.drawImage(image, tx, null);
        gfx.dispose();

        // grab the image memory and stuff it into a direct byte buffer
        ByteBuffer scratch = ByteBuffer.allocateDirect(
            (hasAlpha ? 4 : 3) * twidth * theight).order(ByteOrder.nativeOrder());
        byte data[] = (byte[])tex.getRaster().getDataElements(0, 0, twidth, theight, null);
        scratch.clear();
        scratch.put(data);
        scratch.flip();
        Image textureImage = new Image();
        textureImage.setType(hasAlpha ? Image.RGBA8888 : Image.RGB888);
        textureImage.setWidth(twidth);
        textureImage.setHeight(theight);
        textureImage.setData(scratch);

        setImage(textureImage);

        // make sure we have a unique default color object
        getBatch(0).getDefaultColor().set(ColorRGBA.white);
    }

    /**
     * Creates an image of the specified size, using the supplied JME image data. The image should
     * be a power of two size if OpenGL requires it.
     *
     * @param width the width of the renderable image.
     * @param height the height of the renderable image.
     * @param image the image data.
     */
    public BImage (int width, int height, Image image)
    {
        this(width, height);
        setImage(image);
    }

    /**
     * Returns the width of this image.
     */
    public int getWidth ()
    {
        return _width;
    }

    /**
     * Returns the height of this image.
     */
    public int getHeight ()
    {
        return _height;
    }

    /**
     * Configures this image to use transparency or not (true by default).
     */
    public void setTransparent (boolean transparent)
    {
        if (transparent) {
            setRenderState(blendState);
        } else {
            clearRenderState(RenderState.RS_ALPHA);
        }
        updateRenderState();
    }

    /**
     * Configures the image data to be used by this image.
     */
    public void setImage (Image image)
    {
        // free our old texture as appropriate
        if (_referents > 0) {
            releaseTexture();
        }

        Texture texture = new Texture();
        texture.setImage(image);

        _twidth = image.getWidth();
        _theight = image.getHeight();

        texture.setFilter(Texture.FM_LINEAR);
        texture.setMipmapState(Texture.MM_LINEAR);
        _tstate.setTexture(texture);
        _tstate.setEnabled(true);
        _tstate.setCorrection(TextureState.CM_AFFINE);
        setRenderState(_tstate);
        updateRenderState();

        // preload the new texture
        if (_referents > 0) {
            acquireTexture();
        }
    }

    /**
     * Configures our texture coordinates to the specified subimage. This does not normally need to
     * be called, but if one is stealthily using a BImage as a quad, then it does.
     */
    public void setTextureCoords (int sx, int sy, int swidth, int sheight)
    {
        float lx = sx / (float)_twidth;
        float ly = sy / (float)_theight;
        float ux = (sx+swidth) / (float)_twidth;
        float uy = (sy+sheight) / (float)_theight;

        FloatBuffer tcoords = getTextureBuffer(0, 0);
        tcoords.clear();
        tcoords.put(lx).put(uy);
        tcoords.put(lx).put(ly);
        tcoords.put(ux).put(ly);
        tcoords.put(ux).put(uy);
        tcoords.flip();
    }

    /**
     * Renders this image at the specified coordinates.
     */
    public void render (Renderer renderer, int tx, int ty, float alpha)
    {
        render(renderer, tx, ty, _width, _height, alpha);
    }

    /**
     * Renders this image at the specified coordinates, scaled to the specified size.
     */
    public void render (Renderer renderer, int tx, int ty, int twidth, int theight, float alpha)
    {
        render(renderer, 0, 0, _width, _height, tx, ty, twidth, theight, alpha);
    }

    /**
     * Renders a region of this image at the specified coordinates.
     */
    public void render (Renderer renderer, int sx, int sy,
                        int swidth, int sheight, int tx, int ty, float alpha)
    {
        render(renderer, sx, sy, swidth, sheight, tx, ty, swidth, sheight, alpha);
    }

    /**
     * Renders a region of this image at the specified coordinates, scaled to the specified size.
     */
    public void render (Renderer renderer, int sx, int sy, int swidth, int sheight,
                        int tx, int ty, int twidth, int theight, float alpha)
    {
        if (_referents == 0) {
            Log.log.warning("Unreferenced image rendered " + this + "!");
            Thread.dumpStack();
            return;
        }

        setTextureCoords(sx, sy, swidth, sheight);

        resize(twidth, theight);
        localTranslation.x = tx + twidth/2f;
        localTranslation.y = ty + theight/2f;
        updateGeometricState(0, true);

        getBatch(0).getDefaultColor().a = alpha;
        draw(renderer);
    }

    /**
     * Notes that something is referencing this image and will subsequently call {@link #render} to
     * render the image. <em>This must be paired with a call to {@link #release}.</em>
     */
    public void reference ()
    {
        if (_referents++ == 0) {
            acquireTexture();
        }
    }

    /**
     * Unbinds our underlying texture from OpenGL, removing the data from graphics memory. This
     * should be done when the an image is no longer being displayed. The image will automatically
     * rebind next time it is rendered.
     */
    public void release ()
    {
        if (_referents == 0) {
            Log.log.warning("Unreferenced image released " + this + "!");
            Thread.dumpStack();

        } else if (--_referents == 0) {
            releaseTexture();
        }
    }

    /**
     * Helper constructor.
     */
    protected BImage (int width, int height)
    {
        super("name", width, height);
        _width = width;
        _height = height;
        _tstate = DisplaySystem.getDisplaySystem().getRenderer().createTextureState();
        setTransparent(true);
    }

    protected void acquireTexture ()
    {
        if (_tstate.getNumberOfSetTextures() > 0) {
            _texturePool.acquireTextures(_tstate);
        }
    }

    protected void releaseTexture ()
    {
        if (_tstate.getNumberOfSetTextures() > 0) {
            _texturePool.releaseTextures(_tstate);
        }
    }

    /** Rounds the supplied value up to a power of two. */
    protected static int nextPOT (int value)
    {
        return (Integer.bitCount(value) > 1) ? (Integer.highestOneBit(value) << 1) : value;
    }

    protected TextureState _tstate;
    protected int _width, _height;
    protected int _twidth, _theight;
    protected int _referents;

    protected static boolean _supportsNonPowerOfTwo;

    protected static TexturePool _texturePool = new TexturePool() {
        public void acquireTextures (TextureState tstate) {
            tstate.apply(); // preload
        }
        public void releaseTextures (TextureState tstate) {
            tstate.deleteAll();
        }
    };

    static {
        blendState = DisplaySystem.getDisplaySystem().getRenderer().createAlphaState();
        blendState.setBlendEnabled(true);
        blendState.setSrcFunction(AlphaState.SB_SRC_ALPHA);
        blendState.setDstFunction(AlphaState.DB_ONE_MINUS_SRC_ALPHA);
        blendState.setEnabled(true);
        _supportsNonPowerOfTwo = GLContext.getCapabilities().GL_ARB_texture_non_power_of_two;
    }
}
