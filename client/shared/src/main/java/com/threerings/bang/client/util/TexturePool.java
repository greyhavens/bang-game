//
// $Id$

package com.threerings.bang.client.util;

import com.jmex.bui.BImage;

import com.threerings.bang.util.BasicContext;

/**
 * Maintains a pool of textures for the BUI image backing.
 *
 * <p>jME3 cutover (Phase 2, cluster 6): the fork implementation recycled raw OpenGL texture ids
 * (poking the gdx renderer's {@code TextureStateRecord} and calling {@code GL11.glTexSubImage2D}
 * directly — the "backend leakage" the migration map flags as DROP). jME3 uploads textures lazily
 * and reclaims GL names itself, and the {@code BImage.TexturePool} seam in bui was already retyped
 * to an engine-neutral {@code Object handle} that "does no eager pooling". This implementation is
 * therefore a no-op pool: it satisfies the {@link BImage.TexturePool} contract so {@code BangUI}'s
 * install site keeps working, while delegating all texture lifecycle to jME3.
 */
public class TexturePool
    implements BImage.TexturePool
{
    public TexturePool (BasicContext ctx, int maxSize)
    {
        _ctx = ctx;
        _maxSize = maxSize;
    }

    // documentation inherited from interface BImage.TexturePool
    public void acquireTextures (Object handle)
    {
        // no-op: jME3 uploads textures lazily on first render.
    }

    // documentation inherited from interface BImage.TexturePool
    public void releaseTextures (Object handle)
    {
        // no-op: jME3 reclaims GL texture names itself.
    }

    protected BasicContext _ctx;

    /** Retained for API compatibility; jME3 manages texture memory itself. */
    protected int _maxSize;
}
