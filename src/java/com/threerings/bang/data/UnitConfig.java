//
// $Id$

package com.threerings.bang.data;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Properties;

import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.Terrain;
import com.threerings.bang.util.BangUtil;

import static com.threerings.bang.Log.log;

/**
 * Loads and manages unit configuration information.
 */
public class UnitConfig
{
    /** Defines a unit's modality. */
    public enum Mode {
        GROUND, AIR, RANGE
    };

    /** Defines a unit's make. */
    public enum Make {
        HUMAN, STEAM, SPIRIT
    };

   /** Defines a unit's rank. */
    public enum Rank {
        NORMAL, SPECIAL, BIGSHOT
    }

    /** The total number of modes. */
    public static final int MODE_COUNT = EnumSet.allOf(Mode.class).size();

    /** The total number of makes. */
    public static final int MAKE_COUNT = EnumSet.allOf(Make.class).size();

    /** The total number of terrain types. */
    public static final int TERRAIN_COUNT = EnumSet.allOf(Terrain.class).size();

    /** The name of this unit type (ie. <code>gunslinger</code>, etc.). */
    public String type;

    /** The modality of this unit: {@link #GROUND}, {@link #AIR} or {@link
     * #RANGE}. */
    public Mode mode;

    /** The make of this unit: {@link #HUMAN}, {@link #STEAM} or {@link
     * #SPIRIT}. */
    public Make make;

    /** The rank of this unit: {@link #NORMAL}, {@link #SPECIAL} or {@link
     * #BIGSHOT}. */
    public Rank rank;

    /** The distance this unit can see. */
    public int sightDistance;

    /** The distance this unit can move. */
    public int moveDistance;

    /** The minimum distance this unit can shoot. */
    public int minFireDistance;

    /** The maximum distance this unit can shoot. */
    public int maxFireDistance;

    /** The percentage at which this unit deals a returning fire shot to
     * the shooting unit when fired upon. */
    public int returnFire;

    /** This unit's base damage. */
    public int damage;

    /** The number of ticks into the past to record as this piece's first
     * move, thereby making them available for play sooner. */
    public int initiative;

    /** The type of unit this unit duplicates into. */
    public String dupeType;

    /** The cost of this unit in scrip. */
    public int scripCost;

    /** The cost of this unit in gold. */
    public int goldCost;

    /** Our damage adjustments versus other modes and makes. */
    public int[] damageAdjust = new int[MODE_COUNT + MAKE_COUNT];

    /** Our defense adjustments versus other modes and makes. */
    public int[] defenseAdjust = new int[MODE_COUNT + MAKE_COUNT];

    /** The cost of movement over each type of terrain. */
    public int[] movementAdjust = new int[TERRAIN_COUNT];

    /** A custom class for this unit, if one was specified. */
    public String unitClass;

    /**
     * Computes and returns the damage adjustment to be used when a unit
     * of this type attacks a unit of the specified type.
     */
    public int getDamageAdjust (UnitConfig target)
    {
        return damageAdjust[target.mode.ordinal()] +
            damageAdjust[MODE_COUNT + target.make.ordinal()];
    }

    /**
     * Computes and returns the defense adjustment to be used when a unit
     * of this type is attacked by a unit of the specified type.
     */
    public int getDefenseAdjust (UnitConfig attacker)
    {
        return defenseAdjust[attacker.mode.ordinal()] +
            defenseAdjust[MODE_COUNT + attacker.make.ordinal()];
    }

    /** Returns a translatable name for this unit. */
    public String getName ()
    {
        return MessageBundle.qualify("units", "m." + type);
    }

    /** Returns a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

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

    /**
     * Returns a filtered array of configurations for all unit types
     * accessible in the specified town of the specified rank.
     */
    public static UnitConfig[] getTownUnits (String townId, Rank rank)
    {
        UnitConfig[] units = getTownUnits(townId);
        ArrayList<UnitConfig> list = new ArrayList<UnitConfig>();
        for (int ii = 0; ii < units.length; ii++) {
            if (units[ii].rank == rank) {
                list.add(units[ii]);
            }
        }
        return list.toArray(new UnitConfig[list.size()]);
    }

    public static void main (String[] args)
    {
        for (UnitConfig config : _types.values()) {
            System.err.println("" + config);
        }
    }

    protected static void registerUnit (String type)
    {
        // load up the properties file for this unit
        Properties props = BangUtil.resourceToProperties(
            "rsrc/units/" + type + "/unit.properties");

        // fill in a config instance from the properties file
        UnitConfig config = new UnitConfig();
        config.type = type;
        config.unitClass = props.getProperty("class");

        String modestr =
            BangUtil.requireProperty(type, props, "mode").toUpperCase();
        try {
            config.mode = Enum.valueOf(Mode.class, modestr);
        } catch (Exception e) {
            log.warning("Invalid mode specified [type=" + type +
                        ", mode=" + modestr + "].");
        }
        String makestr =
            BangUtil.requireProperty(type, props, "make").toUpperCase();
        try {
            config.make = Enum.valueOf(Make.class, makestr);
        } catch (Exception e) {
            log.warning("Invalid make specified [type=" + type +
                        ", make=" + makestr + "].");
        }
        String rankstr =
            BangUtil.requireProperty(type, props, "rank").toUpperCase();
        try {
            config.rank = Enum.valueOf(Rank.class, rankstr);
        } catch (Exception e) {
            log.warning("Invalid rank specified [type=" + type +
                        ", rank=" + rankstr + "].");
        }

        config.sightDistance = BangUtil.getIntProperty(type, props, "sight", 5);
        config.moveDistance = BangUtil.getIntProperty(type, props, "move", 1);
        config.minFireDistance =
            BangUtil.getIntProperty(type, props, "min_fire", 1);
        config.maxFireDistance =
            BangUtil.getIntProperty(type, props, "max_fire", 1);
        config.returnFire =
            BangUtil.getIntProperty(type, props, "return_fire", 0);
        config.damage = BangUtil.getIntProperty(type, props, "damage", 25);

        config.initiative =
            BangUtil.getIntProperty(type, props, "initiative", 0);

        config.dupeType = props.getProperty("dupe_type", type);

        config.scripCost = BangUtil.getIntProperty(type, props, "scrip_cost", 0);
        config.goldCost = BangUtil.getIntProperty(type, props, "gold_cost", 0);

        int idx = 0;
        for (Mode mode : EnumSet.allOf(Mode.class)) {
            String key = mode.toString().toLowerCase();
            config.damageAdjust[mode.ordinal()] = BangUtil.getIntProperty(
                type, props, "damage." + key, 0);
            config.defenseAdjust[mode.ordinal()] = BangUtil.getIntProperty(
                type, props, "defense." + key, 0);
        }
        for (Make make : EnumSet.allOf(Make.class)) {
            String key = make.toString().toLowerCase();
            config.damageAdjust[MODE_COUNT + make.ordinal()] =
                BangUtil.getIntProperty(type, props, "damage." + key, 0);
            config.defenseAdjust[MODE_COUNT + make.ordinal()] =
                BangUtil.getIntProperty(type, props, "defense." + key, 0);
        }

        for (Terrain terrain : EnumSet.allOf(Terrain.class)) {
            String key = terrain.toString().toLowerCase();
            config.movementAdjust[terrain.ordinal()] =
                BangUtil.getIntProperty(type, props, "movement." + key, 0);
        }

        // map this config into the proper towns
        String towns = BangUtil.requireProperty(type, props, "towns");
        boolean andSoOn = false;
        for (int ii = 0; ii < BangCodes.TOWN_IDS.length; ii++) {
            String town = BangCodes.TOWN_IDS[ii];
            if (andSoOn || towns.indexOf(town) != -1) {
                mapTown(town, config);
                andSoOn = andSoOn || (towns.indexOf(town + "+") != -1);
            }
        }

        // map the type to the config
        _types.put(type, config);
    }

    protected static void mapTown (String town, UnitConfig config)
    {
        UnitConfig[] configs = _townMap.get(town);
        if (configs == null) {
            configs = new UnitConfig[0];
        }
        UnitConfig[] nconfigs = new UnitConfig[configs.length+1];
        System.arraycopy(configs, 0, nconfigs, 0, configs.length);
        nconfigs[configs.length] = config;
        _townMap.put(town, nconfigs);
    }

    /** A mapping from unit type to its configuration. */
    protected static HashMap<String,UnitConfig> _types =
        new HashMap<String,UnitConfig>();

    /** A mapping from town to all units accessible in that town. */
    protected static HashMap<String,UnitConfig[]> _townMap =
        new HashMap<String,UnitConfig[]>();

    static {
        // register our units
        String[] units = BangUtil.resourceToStrings("rsrc/units/units.txt");
        for (int ii = 0; ii < units.length; ii++) {
            registerUnit(units[ii]);
        }
    }
}
