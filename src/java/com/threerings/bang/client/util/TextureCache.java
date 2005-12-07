//
// $Id$

package com.threerings.bang.client.util;

import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import com.jme.image.Texture;

import com.jmex.bui.util.Rectangle;

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
        TextureKey tkey = new TextureKey(path, null);
        WeakReference<Texture> texref = _textures.get(tkey);
        Texture texture;
        if (texref != null && (texture = texref.get()) != null) {
            return texture;
        }

        texture = RenderUtil.createTexture(_ctx.loadImage(path));
        _textures.put(tkey, new WeakReference<Texture>(texture));
        return texture;
    }

    /**
     * Creates a texture using the specified region from the image with the
     * specified path.
     */
    public Texture getTexture (String path, Rectangle region)
    {
        TextureKey tkey = new TextureKey(path, region);
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
        texture = RenderUtil.createTexture(
            _ctx.getImageCache().createImage(subimg));
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
        public Rectangle region;

        public TextureKey (String path, Rectangle region) {
            this.path = path;
            this.region = region;
        }

        public boolean equals (Object other) {
            TextureKey otk = (TextureKey)other;
            return otk.path.equals(path) &&
                ((region == null && otk.region == null) ||
                 (region != null && otk.region != null &&
                  otk.region.equals(region)));
        }

        public int hashCode () {
            return path.hashCode() ^ (region == null ? 0 : region.hashCode());
        }
    }

    protected BasicContext _ctx;
    protected HashMap<TextureKey,WeakReference<Texture>> _textures =
        new HashMap<TextureKey,WeakReference<Texture>>();
}
