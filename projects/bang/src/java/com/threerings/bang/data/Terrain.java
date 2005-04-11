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
    NONE        (-1, " "),

    // normal terrain types
    DIRT        (0, "."),
    MOSS        (1, "*"),
    TALL_GRASS  (2, "|"),
    WATER       (3, "^"),

    // "constructed" terrain types
    LEAF_BRIDGE (4, "$");

    /** The code used when encoding terrain types in the {@link BangBoard}. */
    public int code;

    /** A character that can be used to display this terrain type when
     * dumping a board to the console. */
    public String glyph;

    /** The set of terrain types that can be used when building boards. */
    public static EnumSet<Terrain> STARTERS = EnumSet.complementOf(
        EnumSet.of(NONE, LEAF_BRIDGE));

    /** Converts an integer code back to the appropriate {@link Terrain}
     * enum value. */
    public static Terrain fromCode (int code)
    {
        return _map.get(code);
    }

    Terrain (int code, String glyph)
    {
        this.code = code;
        this.glyph = glyph;
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
