//
// $Id$

package com.threerings.bang.client.util;

import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;

import com.jme.image.Image;
import com.jme.image.Texture;

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
     * Creates a texture from the image with the specified cache.
     */
    public Texture getTexture (String path)
    {
        return getTexture(path, (Colorization[])null);
    }

    /**
     * Creates a texture from the image with the specified path and
     * colorizations.
     */
    public Texture getTexture (String path, Colorization[] zations)
    {
        TextureKey tkey = new TextureKey(path, zations, null);
        WeakReference<Texture> texref = _textures.get(tkey);
        Texture texture;
        if (texref != null && (texture = texref.get()) != null) {
            return texture;
        }

        Image img = (zations == null) ?
            _ctx.getImageCache().getImage(path) :
            _ctx.getImageCache().createImage(
                _ctx.getImageCache().getBufferedImage(path), zations, true);
        texture = RenderUtil.createTexture(img);
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

    protected static class TextureKey
    {
        public String path;
        public Colorization[] zations;
        public Rectangle region;

        public TextureKey (String path, Colorization[] zations,
            Rectangle region) {
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
    }

    protected BasicContext _ctx;
    protected HashMap<TextureKey,WeakReference<Texture>> _textures =
        new HashMap<TextureKey,WeakReference<Texture>>();
}
