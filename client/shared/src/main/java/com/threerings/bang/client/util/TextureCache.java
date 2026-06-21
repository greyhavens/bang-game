//
// $Id$

package com.threerings.bang.client.util;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

import java.awt.image.BufferedImage;

import java.util.Arrays;
import java.util.HashMap;

import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

import com.jmex.bui.util.Rectangle;

import com.threerings.jme.util.ImageCache;
import com.threerings.media.image.Colorization;

import com.threerings.bang.util.BasicContext;
import com.threerings.bang.util.RenderUtil;

import static com.threerings.bang.Log.log;

/**
 * Implements a simple soft reference based texture cache.
 *
 * <p>jME3 cutover (Phase 2, cluster 6): the fork cached {@code com.jme.image.Texture} state objects
 * and hand-managed their GL texture ids (the {@code CachedTexture}/{@code TextureReference}
 * id-sharing machinery, plus a dummy {@code TextureState} to delete ids). jME3 owns texture upload
 * and GL-name lifecycle, so all of that is gone: the cache holds references to {@link Texture2D}
 * objects built from the jME3 {@link Image}s the {@link ImageCache} produces.
 *
 * <p>These are {@link SoftReference}s, not weak: a weak-ref cache is cleared at <em>every</em> GC,
 * so under heavy churn (e.g. Indian Post spawning/clearing many colorized bonus sprites) a recolored
 * texture became GC-eligible the moment its sprite was removed, jME3 freed its native GL image, and
 * any still-live sprite sharing that {@link ImageCache} {@link Image} then rendered a blank (white)
 * model -- intermittently, per instance. Soft references keep cached textures alive until actual
 * memory pressure, eliminating that race. (Same fix as the {@code ModelCache}/{@code PrototypeCache}
 * weak→soft change.) The public {@code getTexture(...)}/{@code createTexture()} surface is preserved.
 *
 * <p><b>Single-{@link Image}-per-key invariant (white-bonus fix).</b> Soft-referencing the
 * {@link Texture2D} alone is not sufficient: a model instance is a {@code Spatial.clone()} whose
 * cloned {@code Material}'s {@code DiffuseMap} {@code MatParamTexture} keeps pointing at the
 * <em>same</em> cached {@code Texture2D} (jME3's {@code MatParam.clone()} is a shallow copy that
 * shares the {@code Texture} value), so a live sprite strongly reaches the cached {@code Image}.
 * But the {@code _textures} {@code SoftReference} can still be cleared under memory pressure while
 * those live sprites hold the {@code Texture2D}; the very next {@code getTexture} for that key then
 * misses and builds a <em>second</em> {@code Image} (a second GL id) for a path that already has a
 * live one. jME3's {@code NativeObjectManager} keys its cleanup map on the GL id
 * ({@code Image.getUniqueId()}); when the first {@code Image} is later freed and the driver
 * <em>recycles</em> that GL name onto the second (still-live) {@code Image}, a stale phantom for the
 * old id runs {@code realObj.resetObject()} on the live {@code Image} -> {@code id == -1} -> a
 * permanently white but correctly-named texture on a live sprite (the observed {@code img=id-1}).
 * The fork avoided this entirely by sharing one GL id per cache key across every clone (its
 * {@code CachedTexture.textureId int[]}), freed only when the cache entry cleared. We restore that
 * invariant here by canonicalizing the backing {@code Image} per key in {@link #_images}: a rebuilt
 * {@code Texture2D} reuses the still-live {@code Image} (one GL id per key for its whole lifetime)
 * instead of minting a duplicate, so a live sprite's id can never be recycled out from under it.
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
    public Texture2D getTexture (String path)
    {
        return getTexture(path, (Colorization[])null, 1f);
    }

    /**
     * Creates a texture from the image with the specified path and scale.
     */
    public Texture2D getTexture (String path, float scale)
    {
        return getTexture(path, (Colorization[])null, scale);
    }

    /**
     * Creates a texture from the image with the specified path and colorizations.
     */
    public Texture2D getTexture (String path, Colorization[] zations)
    {
        return getTexture(path, zations, 1f);
    }

    /**
     * Creates a texture from the image with the specified path, colorizations, and scale.
     */
    public Texture2D getTexture (String path, Colorization[] zations, float scale)
    {
        TextureKey tkey = new TextureKey(path, zations, null);
        SoftReference<Texture2D> ref = _textures.get(tkey);
        Texture2D texture = (ref == null) ? null : ref.get();
        if (texture != null) {
            return texture;
        }

        // reuse the still-live Image for this key if there is one (see class javadoc): the
        // SoftReference above may have been cleared while live sprites still hold the Texture2D
        // (and thus this Image); rebuilding a second Image here would create a second GL id for
        // the same path and let the driver recycle the freed id onto a live texture.
        Image img = getCachedImage(tkey);
        if (img == null) {
            // if the image is not recolorable, try again without the colorizations
            if (zations != null) {
                BufferedImage bimg = _ctx.getImageCache().getBufferedImage(path);
                if (bimg.getType() != BufferedImage.TYPE_BYTE_INDEXED) {
                    return getTexture(path, scale);
                }
                img = ImageCache.createImage(bimg, zations, scale, true);
            } else {
                img = _ctx.getImageCache().getImage(path, scale);
            }
            _images.put(tkey, new WeakReference<Image>(img));
        }
        texture = new Texture2D();
        RenderUtil.configureTexture(texture, img);
        texture.setName(path);
        _textures.put(tkey, new SoftReference<Texture2D>(texture));
        return texture;
    }

    /**
     * Creates a texture using the specified region from the image with the specified path.
     */
    public Texture2D getTexture (String path, Rectangle region)
    {
        return getTexture(path, null, region);
    }

    /**
     * Creates a texture using the specified region from the image with the specified path.
     */
    public Texture2D getTexture (
        String path, Colorization[] zations, Rectangle region)
    {
        TextureKey tkey = new TextureKey(path, zations, region);
        SoftReference<Texture2D> ref = _textures.get(tkey);
        Texture2D texture = (ref == null) ? null : ref.get();
        if (texture != null) {
            return texture;
        }

        // reuse the still-live Image for this key if there is one (single-Image-per-key invariant)
        Image img = getCachedImage(tkey);
        if (img == null) {
            BufferedImage image = _ctx.getImageCache().getBufferedImage(path);
            if (region.width != region.height) {
                log.warning("Requested to create sub-image texture of non-equal width and height",
                            "path", path, "region", region);
            }

            BufferedImage subimg = image.getSubimage(
                region.x, region.y, region.width, region.height);
            img = (zations == null) ?
                ImageCache.createImage(subimg, true) :
                ImageCache.createImage(subimg, zations, true);
            _images.put(tkey, new WeakReference<Image>(img));
        }
        texture = new Texture2D();
        RenderUtil.configureTexture(texture, img);
        texture.setName(path);
        _textures.put(tkey, new SoftReference<Texture2D>(texture));
        return texture;
    }

    /**
     * Returns the canonical backing {@link Image} for the given key if one is still live, else
     * {@code null}. The {@link #_images} entries are {@link WeakReference}s: an {@code Image} stays
     * resident exactly as long as some live {@link Texture2D} (and hence some live sprite material)
     * still references it, which is precisely the window in which rebuilding a duplicate {@code
     * Image} would be unsafe. Once the last referencing texture is gone the {@code Image} is
     * collected normally and jME3 frees its (then unused) GL id.
     */
    protected Image getCachedImage (TextureKey tkey)
    {
        WeakReference<Image> iref = _images.get(tkey);
        return (iref == null) ? null : iref.get();
    }

    /**
     * Creates a texture using the specified "tile" from the image with the specified path.
     */
    public Texture2D getTexture (String path, int tileWidth, int tileHeight,
                               int tilesPerRow, int tileIndex)
    {
        int tx = tileIndex % tilesPerRow, ty = tileIndex / tilesPerRow;
        Rectangle region = new Rectangle(
            tx * tileWidth, ty * tileHeight, tileWidth, tileHeight);
        return getTexture(path, region);
    }

    /**
     * Creates a new, empty texture. The caller configures it (typically via
     * {@link RenderUtil#configureTexture}). jME3 reclaims the GL texture when this object is no
     * longer referenced.
     */
    public Texture2D createTexture ()
    {
        return new Texture2D();
    }

    /**
     * Computes the count of resident and non-resident cached textures (debug aid).
     */
    public void dumpResidence ()
    {
        int live = 0, flushed = 0;
        for (SoftReference<Texture2D> ref : _textures.values()) {
            if (ref.get() == null) {
                flushed++;
            } else {
                live++;
            }
        }
        log.info("TextureCache live:" + live + " flushed:" + flushed);
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
    protected HashMap<TextureKey, SoftReference<Texture2D>> _textures =
        new HashMap<TextureKey, SoftReference<Texture2D>>();

    /** The canonical backing {@link Image} per key, weakly held so a single GL id is shared by
     * every {@link Texture2D} built for that key for as long as any is live (white-bonus fix). */
    protected HashMap<TextureKey, WeakReference<Image>> _images =
        new HashMap<TextureKey, WeakReference<Image>>();
}
