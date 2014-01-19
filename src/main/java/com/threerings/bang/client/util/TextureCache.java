//
// $Id$

package com.threerings.bang.client.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import java.awt.image.BufferedImage;

import java.nio.IntBuffer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.scene.state.TextureState;
import com.jme.util.geom.BufferUtils;

import com.jmex.bui.util.Rectangle;

import com.samskivert.util.Interval;

import com.threerings.jme.util.ImageCache;
import com.threerings.media.image.Colorization;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.log;

/**
 * Implements a simple weak reference based texture cache.
 */
public class TextureCache
{
    public TextureCache (BasicContext ctx)
    {
        _ctx = ctx;
        _dtstate = ctx.getRenderer().createTextureState();

        // create the interval to flush cleared textures
        new Interval(ctx.getApp()) {
            public void expired () {
                Reference<? extends CachedTexture> ref;
                while ((ref = _cleared.poll()) != null) {
                    TextureReference.class.cast(ref).flush();
                }
            }
        }.schedule(FLUSH_INTERVAL, true);
    }

    /**
     * Creates a texture from the image with the specified path.
     */
    public Texture getTexture (String path)
    {
        return getTexture(path, (Colorization[])null, 1f);
    }

    /**
     * Creates a texture from the image with the specified path and scale.
     */
    public Texture getTexture (String path, float scale)
    {
        return getTexture(path, (Colorization[])null, scale);
    }

    /**
     * Creates a texture from the image with the specified path and
     * colorizations.
     */
    public Texture getTexture (String path, Colorization[] zations)
    {
        return getTexture(path, zations, 1f);
    }

    /**
     * Creates a texture from the image with the specified path,
     * colorizations, and scale.
     */
    public Texture getTexture (String path, Colorization[] zations, float scale)
    {
        TextureKey tkey = new TextureKey(path, zations, null);
        TextureReference ref = _textures.get(tkey);
        CachedTexture texture = (ref == null) ? null : ref.get();
        if (texture != null) {
            return texture;
        }

        // if the image is not recolorable, try again without the colorizations
        Image img;
        if (zations != null) {
            BufferedImage bimg = _ctx.getImageCache().getBufferedImage(path);
            if (bimg.getType() != BufferedImage.TYPE_BYTE_INDEXED) {
                return getTexture(path, scale);
            }
            img = ImageCache.createImage(bimg, zations, scale, true);
        } else {
            img = _ctx.getImageCache().getImage(path, scale);
        }
        texture = new CachedTexture();
        RenderUtil.configureTexture(texture, img);
        texture.setImageLocation(path);
        _textures.put(tkey, new TextureReference(texture));
        return texture;
    }

    /**
     * Creates a texture using the specified region from the image with the
     * specified path.
     */
    public Texture getTexture (String path, Rectangle region)
    {
        return getTexture(path, null, region);
    }

    /**
     * Creates a texture using the specified region from the image with the
     * specified path.
     */
    public Texture getTexture (
        String path, Colorization[] zations, Rectangle region)
    {
        TextureKey tkey = new TextureKey(path, zations, region);
        TextureReference ref = _textures.get(tkey);
        CachedTexture texture = (ref == null) ? null : ref.get();
        if (texture != null) {
            return texture;
        }

        BufferedImage image = _ctx.getImageCache().getBufferedImage(path);
        if (region.width != region.height) {
            log.warning("Requested to create sub-image texture of non-equal width and height",
                        "path", path, "region", region);
        }

        BufferedImage subimg = image.getSubimage(
            region.x, region.y, region.width, region.height);
        Image img = (zations == null) ?
            ImageCache.createImage(subimg, true) :
            ImageCache.createImage(subimg, zations, true);
        texture = new CachedTexture();
        RenderUtil.configureTexture(texture, img);
        texture.setImageLocation(path);
        _textures.put(tkey, new TextureReference(texture));
        return texture;
    }

    /**
     * Creates a texture using the specified "tile" from the image with the
     * specified path.
     */
    public Texture getTexture (String path, int tileWidth, int tileHeight,
                               int tilesPerRow, int tileIndex)
    {
        int tx = tileIndex % tilesPerRow, ty = tileIndex / tilesPerRow;
        Rectangle region = new Rectangle(
            tx * tileWidth, ty * tileHeight, tileWidth, tileHeight);
        return getTexture(path, region);
    }

    /**
     * Creates a texture that will be deleted when it is no longer accessible.
     */
    public Texture createTexture ()
    {
        return new TextureReference(new CachedTexture()).get();
    }

    /**
     * Computes the count and size of our resident and non-resident cached
     * textures.
     */
    public void dumpResidence ()
    {
        int[] counts = new int[4], bytes = new int[4];
        for (Map.Entry<TextureKey, TextureReference> entry : _textures.entrySet()) {
            TextureKey key = entry.getKey();
            Texture texture = entry.getValue().get();
            if (texture == null) {
//                 log.info("Flushed " + key.path + ":" + key.region);
                counts[0]++;
                continue;
            }

            Image image = texture.getImage();
            int size = (image == null) ? 0 : image.getData().capacity();
            int texid = texture.getTextureId();
            if (texid <= 0) {
//                 log.info("Not loaded " + key.path + ":" + key.region);
                int idx = (size == 0) ? 1 : 2;
                counts[idx]++;
                bytes[idx] += size;
                continue;
            }

//             GL11.glGetTexParameter(texid, GL11.GL_TEXTURE_RESIDENT, _qbuf);
            if (size == 0) {
                log.warning("Loadaed texture has no image?", "key", key);
            } else {
                counts[3]++;
                bytes[3] += size;
            }

//             log.info("Resident? " + key.path + ":" + key.region + ":" + texid +
//                      " " + _qbuf.get(0));
        }

        log.info("FL:" + counts[0] + " NL0:" + counts[1] +
                 " NL1:" + counts[2] + "/" + bytes[2] +
                 " LD:" + counts[3] + "/" + bytes[3]);
    }

    /** Retains a reference to the cached texture's texture id in order to delete the OpenGL
     * texture when the reference is cleared. */
    protected class TextureReference extends WeakReference<CachedTexture>
    {
        public TextureReference (CachedTexture texture)
        {
            super(texture, _cleared);
            _textureId = texture.textureId;
        }

        /**
         * Unloads the texture.
         */
        public void flush ()
        {
            if (_textureId[0] > 0) {
                _dtstate.deleteTextureId(_textureId[0]);
                _textureId[0] = 0;
            }
        }

        protected int[] _textureId;
    }

    /** Ensures that clones of the texture share the same texture id reference used by the
     * {@link TextureReference} as well as a reference to the prototype (to prevent its being
     * garbage collected). */
    protected static class CachedTexture extends Texture
    {
        /** The texture id reference shared by the prototype and its clones. */
        public int[] textureId;

        /** Constructor for prototype textures. */
        public CachedTexture ()
        {
            textureId = new int[1];
        }

        /** Constructor for cloned textures. */
        public CachedTexture (float aniso, CachedTexture prototype)
        {
            super(aniso);
            textureId = prototype.textureId;
            _prototype = prototype;
        }

        @Override // documentation inherited
        public int getTextureId ()
        {
            return textureId[0];
        }

        @Override // documentation inherited
        public void setTextureId (int textureId)
        {
            this.textureId[0] = textureId;
        }

        @Override // documentation inherited
        public Texture createSimpleClone ()
        {
            return createSimpleClone(new CachedTexture(
                getAnisoLevel(), _prototype == null ? this : _prototype));
        }

        protected CachedTexture _prototype;
    }

    protected static class TextureKey
    {
        public String path;
        public Colorization[] zations;
        public Rectangle region;

        public TextureKey (
            String path, Colorization[] zations, Rectangle region) {
            this.path = path;
            this.zations = zations;
            this.region = region;
        }

        public boolean equals (Object other) {
            TextureKey otk = (TextureKey)other;
            return otk.path.equals(path) &&
                Arrays.equals(otk.zations, zations) &&
                ((region == null && otk.region == null) ||
                 (region != null && otk.region != null &&
                  otk.region.equals(region)));
        }

        public int hashCode () {
            return path.hashCode() ^ Arrays.hashCode(zations) ^
                (region == null ? 0 : region.hashCode());
        }

        public String toString () {
            return path + ":" + zations + ":" + region;
        }
    }

    protected BasicContext _ctx;

    /** The cached textures. */
    protected HashMap<TextureKey, TextureReference> _textures =
        new HashMap<TextureKey, TextureReference>();

    /** The queue of textures to destroy. */
    protected ReferenceQueue<CachedTexture> _cleared = new ReferenceQueue<CachedTexture>();

    /** A dummy texture state used to delete textures. */
    protected TextureState _dtstate;

    protected IntBuffer _qbuf = BufferUtils.createIntBuffer(4);

    /** The rate at which to check for cleared textures to destroy. */
    protected static final long FLUSH_INTERVAL = 5000L;
}
