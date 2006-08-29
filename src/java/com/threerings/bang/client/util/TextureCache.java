//
// $Id$

package com.threerings.bang.client.util;

import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.nio.IntBuffer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.jme.image.Image;
import com.jme.image.Texture;
import com.jme.util.geom.BufferUtils;

import com.jmex.bui.util.Rectangle;

import com.threerings.media.image.Colorization;
import com.threerings.media.image.ImageUtil;

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
    public Texture getTexture (String path, Colorization[] zations,
        float scale)
    {
        TextureKey tkey = new TextureKey(path, zations, null);
        WeakReference<Texture> texref = _textures.get(tkey);
        Texture texture;
        if (texref != null && (texture = texref.get()) != null) {
            return texture;
        }

        // if the image is not recolorable, try again without the colorizations
        Image img;
        if (zations != null) {
            BufferedImage bimg = _ctx.getImageCache().getBufferedImage(path);
            if (bimg.getType() != BufferedImage.TYPE_BYTE_INDEXED) {
                return getTexture(path, scale);
            }
            img = _ctx.getImageCache().createImage(bimg, zations, scale, true);
        } else {
            img = _ctx.getImageCache().getImage(path, scale);
        }
        texture = RenderUtil.createTexture(img);
        texture.setImageLocation(path);
        _textures.put(tkey, new WeakReference<Texture>(texture));
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
        WeakReference<Texture> texref = _textures.get(tkey);
        Texture texture;
        if (texref != null && (texture = texref.get()) != null) {
            return texture;
        }

        BufferedImage image = _ctx.getImageCache().getBufferedImage(path);
        if (region.width != region.height) {
            log.warning("Requested to create sub-image texture of " +
                        "non-equal width and height [path=" + path +
                        ", region=" + region + "].");
        }

        BufferedImage subimg = image.getSubimage(
            region.x, region.y, region.width, region.height);
        Image img = (zations == null) ?
            _ctx.getImageCache().createImage(subimg, true) :
            _ctx.getImageCache().createImage(subimg, zations, true);
        texture = RenderUtil.createTexture(img);
        texture.setImageLocation(path);
        _textures.put(tkey, new WeakReference<Texture>(texture));
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
     * Computes the count and size of our resident and non-resident cached
     * textures.
     */
    public void dumpResidence ()
    {
        int[] counts = new int[4], bytes = new int[4];
        for (Map.Entry<TextureKey,WeakReference<Texture>> entry :
                 _textures.entrySet()) {
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
                log.warning("Loadaed texture has no image? [key=" + key + "].");
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

    protected HashMap<TextureKey,WeakReference<Texture>> _textures =
        new HashMap<TextureKey,WeakReference<Texture>>();

    protected IntBuffer _qbuf = BufferUtils.createIntBuffer(4);
}
