//
// $Id$

package com.threerings.bang.data;

import java.util.HashMap;
import java.util.Properties;

import com.samskivert.util.StringUtil;
import com.threerings.bang.util.BangUtil;

import static com.threerings.bang.Log.log;

/**
 * Loads and manages building configuration information.
 */
public class BuildingConfig
{
    /** The name of this building type (ie. <code>saloon</code>, etc.). */
    public String type;

    /** The width of this building in tiles. */
    public int width;

    /** The height of this building in tiles. */
    public int height;

    /** A custom class for this building, if one was specified. */
    public String buildingClass;

    /** Returns a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    /**
     * Returns the building configuration for the specified building type.
     */
    public static BuildingConfig getConfig (String type)
    {
        return _types.get(type);
    }

    /**
     * Returns an array of configurations for all building types
     * accessible in the specified town.
     */
    public static BuildingConfig[] getTownBuildings (String townId)
    {
        return _townMap.get(townId);
    }

    public static void main (String[] args)
    {
        for (BuildingConfig config : _types.values()) {
            System.err.println("" + config);
        }
    }

    protected static void registerBuilding (String type)
    {
        // load up the properties file for this building
        Properties props = BangUtil.resourceToProperties(
            "rsrc/buildings/" + type + "/building.properties");

        // fill in a config instance from the properties file
        BuildingConfig config = new BuildingConfig();
        config.type = type;
        config.buildingClass = props.getProperty("class");

        config.width = BangUtil.getIntProperty(type, props, "width", 1);
        config.height = BangUtil.getIntProperty(type, props, "height", 1);

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

    protected static void mapTown (String town, BuildingConfig config)
    {
        BuildingConfig[] configs = _townMap.get(town);
        if (configs == null) {
            configs = new BuildingConfig[0];
        }
        BuildingConfig[] nconfigs = new BuildingConfig[configs.length+1];
        System.arraycopy(configs, 0, nconfigs, 0, configs.length);
        nconfigs[configs.length] = config;
        _townMap.put(town, nconfigs);
    }

    /** A mapping from building type to its configuration. */
    protected static HashMap<String,BuildingConfig> _types =
        new HashMap<String,BuildingConfig>();

    /** A mapping from town to all buildings accessible in that town. */
    protected static HashMap<String,BuildingConfig[]> _townMap =
        new HashMap<String,BuildingConfig[]>();

    static {
        // register our buildings
        String[] bldgs = BangUtil.resourceToStrings(
            "rsrc/buildings/buildings.txt");
        for (int ii = 0; ii < bldgs.length; ii++) {
            registerBuilding(bldgs[ii]);
        }
    }
}
