//
// $Id$

package com.threerings.bang.data;

import java.util.EnumSet;
import java.util.HashMap;

/**
 * Models the different types of terrain that can be found on a board.
 */
public enum Terrain
{
    // indicates a non-playable tile
    NONE        (-1, -1),

    // normal terrain types: DON'T BOOCH increasing terrain code
    BRUSH       (0, 3*BangBoard.BASE_TRAVERSAL/2),
    DARK_DIRT   (1, BangBoard.BASE_TRAVERSAL),
    DIRT        (2, BangBoard.BASE_TRAVERSAL),
    ROAD        (3, BangBoard.BASE_TRAVERSAL),
    ROCKY       (4, 3*BangBoard.BASE_TRAVERSAL/2),
    ROUGH_DIRT  (5, 4*BangBoard.BASE_TRAVERSAL/3),
    SAND        (6, 2*BangBoard.BASE_TRAVERSAL),
    WATER       (7, -1),

    // special "outside the board" tiles
    OUTER       (99, -1),
    RIM         (100, -1);

    /** The code used when encoding terrain types in the {@link BangBoard}. */
    public int code;

    /** The normal traversal cost for this terrain. */
    public int traversalCost;

    /** The set of terrain types that can be used when displaying boards. */
    public static EnumSet<Terrain> RENDERABLE = EnumSet.complementOf(
        EnumSet.of(NONE, RIM));

    /** The set of terrain types that can be used when creating boards. */
    public static EnumSet<Terrain> USABLE = EnumSet.complementOf(
        EnumSet.of(NONE, OUTER, RIM));

    /** Converts an integer code back to the appropriate {@link Terrain}
     * enum value. */
    public static Terrain fromCode (int code)
    {
        return _map.get(code);
    }

    Terrain (int code, int traversalCost)
    {
        this.code = code;
        this.traversalCost = traversalCost;
        map(this);
    }

    protected static void map (Terrain terrain)
    {
        // this method ends up running before the static initializers
        if (_map == null) {
            _map = new HashMap<Integer,Terrain>();
        }
        _map.put(terrain.code, terrain);
    }

    /** Maps the enumeration's code back to the enumeration itself. */
    protected static HashMap<Integer,Terrain> _map;
}
