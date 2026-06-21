//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;

import com.jme3.renderer.RenderManager;
import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;

import com.jmex.bui.backend.BImageBacking;
import com.jmex.bui.backend.BackendProvider;

/**
 * Contains a texture (as a jME3 pixel {@link Image}), its dimensions and the engine-specific
 * backing that draws it. Since the jME3 cutover (Phase 1) this is a plain data/render holder
 * and no longer a scene-graph node.
 */
public class BImage
{
    /** An interface for pooling textures. The jME3 backing does no eager pooling (jME3 uploads
     * lazily and reclaims GL textures itself); a host may install a pool to manage textures.
     * The {@code Object} parameter is the backend's texture handle, typed loosely so this
     * interface stays engine-neutral. */
    public interface TexturePool
    {
        /** Acquires the texture(s) for the supplied backing handle. */
        public void acquireTextures (Object handle);

        /** Releases the texture(s) for the supplied backing handle. */
        public void releaseTextures (Object handle);
    }

    /**
     * Sets the texture pool from which to acquire and release texture objects.
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
        if (!BackendProvider.get().supportsNonPowerOfTwo()) {
            twidth = nextPOT(twidth);
            theight = nextPOT(theight);
        }

        // render the image into a standard ARGB raster we can read pixel-exactly
        boolean hasAlpha = hasAlpha(image);
        BufferedImage tex = new BufferedImage(twidth, theight, BufferedImage.TYPE_INT_ARGB);
        AffineTransform tx = null;
        if (flip) {
            tx = AffineTransform.getScaleInstance(1, -1);
            tx.translate(0, -_height);
        }
        Graphics2D gfx = (Graphics2D) tex.getGraphics();
        gfx.drawImage(image, tx, null);
        gfx.dispose();

        // Build the pixel buffer with explicit R,G,B[,A] byte order and hand it to jME3 as
        // RGBA8/RGB8. We must NOT use jME3's ABGR8 over an AWT TYPE_4BYTE_ABGR raster: jME3 maps
        // ABGR8 to GL_RGBA + GL_UNSIGNED_INT_8_8_8_8, whose packing is endianness-sensitive and on
        // little-endian hardware scrambles the channels (UI rendered with a pink/red cast). RGBA8
        // and RGB8 map to GL_RGBA/GL_RGB + GL_UNSIGNED_BYTE, which is byte-order independent.
        int bpp = hasAlpha ? 4 : 3;
        ByteBuffer scratch = ByteBuffer.allocateDirect(bpp * twidth * theight)
            .order(ByteOrder.nativeOrder());
        int[] argb = tex.getRGB(0, 0, twidth, theight, null, 0, twidth);
        for (int i = 0; i < argb.length; i++) {
            int p = argb[i];
            scratch.put((byte)((p >> 16) & 0xFF)); // R
            scratch.put((byte)((p >> 8) & 0xFF));  // G
            scratch.put((byte)(p & 0xFF));         // B
            if (hasAlpha) {
                scratch.put((byte)((p >> 24) & 0xFF)); // A
            }
        }
        scratch.flip();

        Image.Format format = hasAlpha ? Image.Format.RGBA8 : Image.Format.RGB8;
        Image textureImage = new Image(format, twidth, theight, scratch, ColorSpace.sRGB);

        setImage(textureImage);
    }

    /**
     * Creates an image of the specified size, using the supplied jME3 image data. The image
     * should be a power of two size if OpenGL requires it.
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
        _backing.setTransparent(transparent);
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

        _twidth = image.getWidth();
        _theight = image.getHeight();
        _backing.setImage(image);

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
        _backing.setTextureCoords(sx, sy, swidth, sheight);
    }

    /**
     * Renders this image at the specified coordinates.
     */
    public void render (RenderManager renderer, int tx, int ty, float alpha)
    {
        render(renderer, tx, ty, _width, _height, alpha);
    }

    /**
     * Renders this image at the specified coordinates, scaled to the specified size.
     */
    public void render (RenderManager renderer, int tx, int ty, int twidth, int theight, float alpha)
    {
        render(renderer, 0, 0, _width, _height, tx, ty, twidth, theight, alpha);
    }

    /**
     * Renders a region of this image at the specified coordinates.
     */
    public void render (RenderManager renderer, int sx, int sy,
                        int swidth, int sheight, int tx, int ty, float alpha)
    {
        render(renderer, sx, sy, swidth, sheight, tx, ty, swidth, sheight, alpha);
    }

    /**
     * Renders a region of this image at the specified coordinates, scaled to the specified size.
     */
    public void render (RenderManager renderer, int sx, int sy, int swidth, int sheight,
                        int tx, int ty, int twidth, int theight, float alpha)
    {
        if (_referents == 0) {
            Log.log.warning("Unreferenced image rendered " + this + "!");
            Thread.dumpStack();
            return;
        }

        setTextureCoords(sx, sy, swidth, sheight);
        _backing.render(renderer, tx, ty, twidth, theight, alpha);
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
        _width = width;
        _height = height;
        _backing = BackendProvider.get().createImageBacking(this);
        setTransparent(true);
    }

    protected void acquireTexture ()
    {
        _backing.acquire();
    }

    protected void releaseTexture ()
    {
        _backing.release();
    }

    /** Rounds the supplied value up to a power of two. */
    protected static int nextPOT (int value)
    {
        return (Integer.bitCount(value) > 1) ? (Integer.highestOneBit(value) << 1) : value;
    }

    /** Returns true if the supplied image has an alpha channel. */
    protected static boolean hasAlpha (java.awt.Image image)
    {
        if (image instanceof BufferedImage) {
            return ((BufferedImage)image).getColorModel().hasAlpha();
        }
        PixelGrabber grabber = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            grabber.grabPixels();
            ColorModel model = grabber.getColorModel();
            return (model != null) && model.hasAlpha();
        } catch (InterruptedException e) {
            return false;
        }
    }

    protected BImageBacking _backing;
    protected int _width, _height;
    protected int _twidth, _theight;
    protected int _referents;

    protected static TexturePool _texturePool;
}
