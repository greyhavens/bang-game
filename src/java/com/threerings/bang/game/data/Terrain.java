//
// $Id$

package com.threerings.bang.game.data;

import java.util.EnumSet;
import java.util.HashMap;

/**
 * Models the different types of terrain that can be found on a board.
 */
public enum Terrain
{
    // indicates a non-playable tile
    NONE (-1, -1, 0f),

    // normal terrain types: DON'T BOOCH increasing terrain code
    BRUSH               (0, BangBoard.BASE_TRAVERSAL, 0.2f),
    DARK_DIRT           (1, BangBoard.BASE_TRAVERSAL, 0.5f),
    DIRT                (2, BangBoard.BASE_TRAVERSAL, 0.8f),
    ROAD                (3, BangBoard.BASE_TRAVERSAL, 0.2f),
    ROCKY               (4, BangBoard.BASE_TRAVERSAL, 0.2f),
    ROUGH_DIRT          (5, BangBoard.BASE_TRAVERSAL, 0.5f),
    SAND                (6, BangBoard.BASE_TRAVERSAL, 0.9f),
    WATER               (7, -1, 0f),
    BONE                (8, BangBoard.BASE_TRAVERSAL, 0.2f),
    PRAIRIE_GRASS_GREEN (9, BangBoard.BASE_TRAVERSAL, 0f),
    PRAIRIE_GRASS_GOLD  (10, BangBoard.BASE_TRAVERSAL, 0f),

    // special "outside the board" tiles
    OUTER (99, -1, 0f),
    RIM   (100, -1, 0f);

    /** The code used when encoding terrain types in the {@link BangBoard}. */
    public int code;

    /** The normal traversal cost for this terrain. */
    public int traversalCost;

    /** The amount of stuff that units kick up when they move over this
     * terrain: 0 for none, 1 for lots. */
    public float dustiness;
    
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

    Terrain (int code, int traversalCost, float dustiness)
    {
        this.code = code;
        this.traversalCost = traversalCost;
        this.dustiness = dustiness;
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
