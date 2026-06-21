//
// $Id$

package com.threerings.jme.util;

import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.GeometryComparator;
import com.jme3.renderer.queue.TransparentComparator;
import com.jme3.scene.Geometry;

/**
 * A {@link GeometryComparator} for the transparent bucket that honours an explicit per-geometry
 * render-order key before falling back to the stock back-to-front {@link TransparentComparator}.
 *
 * <p>The 2005-era jME fork drew Bang's multi-texture terrain as a single multi-<em>pass</em> mesh
 * entirely within the opaque (non-transparent) terrain draw, so the whole terrain surface always
 * rendered <em>before</em> the transparent move/attack highlight tiles. The jME3 cutover instead
 * renders each extra terrain "splat" layer as its own alpha-blended {@code Bucket.Transparent}
 * overlay geometry. jME3 sorts the transparent bucket purely back-to-front by bound distance, so on
 * sloped/raised tiles a splat block's centroid can sort nearer the camera than a highlight tile,
 * the splat then paints over the highlight and the highlight appears to "sink under" the terrain
 * (BUG-31).</p>
 *
 * <p>This comparator restores the fork's invariant — terrain before highlights — without disturbing
 * the splat blending (the splat materials, depth state and node hierarchy are unchanged) or the
 * relative order of any other transparent geometry (water, particles, effects, UI). It reads the
 * integer {@link #RENDER_ORDER} user-data key from each geometry (absent == {@link #DEFAULT_ORDER})
 * and sorts ascending by it; geometries that share an order key are ordered exactly as the stock
 * transparent comparator would order them. Terrain geometries are tagged with a negative order so
 * they sort ahead of the (default-order) highlights and everything else.</p>
 */
public class RenderOrderComparator implements GeometryComparator
{
    /** The geometry user-data key (an {@link Integer}) read by this comparator. */
    public static final String RENDER_ORDER = "bang.RenderOrder";

    /** The order assumed for geometries with no {@link #RENDER_ORDER} user data. */
    public static final int DEFAULT_ORDER = 0;

    /** The order used to tag terrain geometry so it draws before highlights/effects. */
    public static final int TERRAIN_ORDER = -10;

    @Override
    public void setCamera (Camera cam)
    {
        _fallback.setCamera(cam);
    }

    @Override
    public int compare (Geometry o1, Geometry o2)
    {
        int order = Integer.compare(orderOf(o1), orderOf(o2));
        return (order != 0) ? order : _fallback.compare(o1, o2);
    }

    protected static int orderOf (Geometry geom)
    {
        Integer order = geom.getUserData(RENDER_ORDER);
        return (order == null) ? DEFAULT_ORDER : order.intValue();
    }

    protected final TransparentComparator _fallback = new TransparentComparator();
}
