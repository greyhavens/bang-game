//
// $Id$

package com.threerings.bang.data;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;

import com.samskivert.util.StringUtil;

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

    /** The distance this unit can shoot. */
    public int fireDistance;

    /** This unit's base damage. */
    public int damage;

    /** The cost of this unit in scrip. */
    public int scripCost;

    /** The cost of this unit in gold. */
    public int goldCost;

    /** Our damage adjustments versus other modes and makes. */
    public int[] damageAdjust = new int[MODE_COUNT + MAKE_COUNT];

    /** Our defense adjustments versus other modes and makes. */
    public int[] defenseAdjust = new int[MODE_COUNT + MAKE_COUNT];

    /** A custom class for this unit, if one was specified. */
    public String unitClass;

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
        Properties props = new Properties();
        String path = "rsrc/units/" + type + "/unit.properties";
        try {
            props.load(
                UnitConfig.class.getClassLoader().getResourceAsStream(path));
        } catch (Exception e) {
            log.log(Level.WARNING, "No config for '" + type + "'?", e);
            return;
        }

        // fill in a config instance from the properties file
        UnitConfig config = new UnitConfig();
        config.type = type;
        config.unitClass = props.getProperty("class");

        String modestr = requireProperty(type, props, "mode").toUpperCase();
        try {
            config.mode = Enum.valueOf(Mode.class, modestr);
        } catch (Exception e) {
            log.warning("Invalid mode specified [type=" + type +
                        ", mode=" + modestr + "].");
        }
        String makestr = requireProperty(type, props, "make").toUpperCase();
        try {
            config.make = Enum.valueOf(Make.class, makestr);
        } catch (Exception e) {
            log.warning("Invalid make specified [type=" + type +
                        ", make=" + makestr + "].");
        }
        String rankstr = requireProperty(type, props, "rank").toUpperCase();
        try {
            config.rank = Enum.valueOf(Rank.class, rankstr);
        } catch (Exception e) {
            log.warning("Invalid rank specified [type=" + type +
                        ", rank=" + rankstr + "].");
        }

        config.sightDistance = getIntProperty(type, props, "sight", 5);
        config.moveDistance = getIntProperty(type, props, "move", 1);
        config.fireDistance = getIntProperty(type, props, "fire", 1);
        config.damage = getIntProperty(type, props, "damage", 25);
        config.scripCost = getIntProperty(type, props, "scrip_cost", 0);
        config.goldCost = getIntProperty(type, props, "gold_cost", 0);

        int idx = 0;
        for (Mode mode : EnumSet.allOf(Mode.class)) {
            String key = mode.toString().toLowerCase();
            config.damageAdjust[mode.ordinal()] = getIntProperty(
                type, props, "damage." + key, 0);
            config.defenseAdjust[mode.ordinal()] = getIntProperty(
                type, props, "defense." + key, 0);
        }
        for (Make make : EnumSet.allOf(Make.class)) {
            String key = make.toString().toLowerCase();
            config.damageAdjust[MODE_COUNT + make.ordinal()] = getIntProperty(
                type, props, "damage." + key, 0);
            config.defenseAdjust[MODE_COUNT + make.ordinal()] = getIntProperty(
                type, props, "defense." + key, 0);
        }

        // map this config into the proper towns
        String towns = requireProperty(type, props, "towns");
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

    protected static String requireProperty (
        String type, Properties props, String key)
    {
        String value = props.getProperty(key);
        if (value == null) {
            log.warning("Missing unit config [type=" + type +
                        ", key=" + key + "].");
            value = "";
        }
        return value;
    }

    protected static int getIntProperty (
        String type, Properties props, String key, int defval)
    {
        String value = props.getProperty(key);
        try {
            return (value != null) ? Integer.parseInt(value) : defval;
        } catch (Exception e) {
            log.warning("Invalid unit config [type=" + type + ", key=" + key +
                        ", value=" + value + "]: " + e);
            return defval;
        }
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

        // register the Frontier Town Big Shots
        registerUnit("cavalry");

        // register the Boom Town units
        registerUnit("windupslinger");
    }
}
