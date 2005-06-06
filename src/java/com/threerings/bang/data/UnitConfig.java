//
// $Id$

package com.threerings.bang.data;

import java.util.HashMap;

/**
 * Loads and manages unit configuration information.
 */
public class UnitConfig
{
    /** The modality for ground units. */
    public static final int GROUND = 0;

    /** The modality for air units. */
    public static final int AIR = 1;

    /** The modality for range units. */
    public static final int RANGE = 2;

    /** The make of human units. */
    public static final int HUMAN = 3;

    /** The make of steam units. */
    public static final int STEAM = 4;

    /** The make of spirit units. */
    public static final int SPIRIT = 5;

    /** The rank of normal playable units. */
    public static final int BASE = 0;

    /** The rank of special units that can't be purchased but turn up in
     * games at special times and can be controlled. */
    public static final int SPECIAL = 1;

    /** The rank of Big Shot units. */
    public static final int BIGSHOT = 2;

    /** The name of this unit type (ie. <code>gunslinger</code>, etc.). */
    public String type;

    /** The modality of this unit: {@link #GROUND}, {@link #AIR} or {@link
     * #RANGE}. */
    public int mode;

    /** The make of this unit: {@link #HUMAN}, {@link #STEAM} or {@link
     * #SPIRIT}. */
    public int make;

    /** The rank of this unit: {@link #BASE}, {@link #SPECIAL} or {@link
     * #BIGSHOT}. */
    public int rank;

    /** The towns in which this unit is available: {@link
     * BangCodes#FRONTIER_TOWN}, etc.. */
    public String[] towns;

    /** The distance this unit can see. */
    public int sightDistance;

    /** The distance this unit can move. */
    public int moveDistance;

    /** The distance this unit can shoot. */
    public int fireDistance;

    /** This unit's base damage. */
    public int damage;

    /** Our damage adjustments versus other modes and makes. */
    public int[] damageAdjust = new int[6];

    /** Our defense adjustments versus other modes and makes. */
    public int[] defenseAdjust = new int[6];

    /**
     * Returns the unit configuration for the specified unit type.
     */
    public static UnitConfig getConfig (String type)
    {
        return _types.get(type);
    }

    /**
     * Returns an array of configurations for all unit types accessible in
     * the specified town.
     */
    public static UnitConfig[] getTownUnits (String townId)
    {
        return _townMap.get(townId);
    }

    protected static void registerUnit (String type)
    {

//         // add the unit to a town set if appropriate
//         if (townId != null) {
//             String[] units = _unitTypes.get(townId);
//             if (units == null) {
//                 units = new String[0];
//             }
//             String[] nunits = new String[units.length+1];
//             System.arraycopy(units, 0, nunits, 0, units.length);
//             nunits[units.length] = proto.getType();
//             _unitTypes.put(townId, nunits);
//         }

//         // map the type to the class
//         _unitMap.put(proto.getType(), proto.getClass());
    }

    /** A mapping from unit type to its configuration. */
    protected static HashMap<String,UnitConfig> _types =
        new HashMap<String,UnitConfig>();

    /** A mapping from town to all units accessible in that town. */
    protected static HashMap<String,UnitConfig[]> _townMap =
        new HashMap<String,UnitConfig[]>();

    static {
        // register the Frontier Town units
        registerUnit("gunslinger");
        registerUnit("dirigible");
        registerUnit("artillery");
        registerUnit("steamgunman");

        // register the Boom Town units
        registerUnit("windupslinger");
    }
}
