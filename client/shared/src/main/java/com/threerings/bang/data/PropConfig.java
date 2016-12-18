//
// $Id$

package com.threerings.bang.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;

import com.samskivert.util.StringUtil;
import com.threerings.bang.util.BangUtil;

/**
 * Loads and manages prop configuration information.
 */
public class PropConfig
{
    /** The name of this prop type (e.g.,
     * <code>frontier_town/buildings/saloon</code>). */
    public String type;

    /** The prop model (usually the same as the prop type). */
    public String model;

    /** The prop variant, where <code>null</code> is the default. */
    public String variant;

    /** The width of this prop in tiles. */
    public int width;

    /** The length of this prop in tiles. */
    public int length;

    /** The height of this prop in tiles. */
    public float height;

    /** The passable elevation of this prop in tiles. */
    public float passElev;

    /** If true, even air units cannot pass over the prop. */
    public boolean tall;

    /** A custom class for this prop, if one was specified. */
    public String propClass;

    /** A scenario for this prop, if one was specified. */
    public String scenario;

    /** Whether or not units can pass over/through the prop. */
    public boolean passable;

    /** Whether or not units can fire through the prop. */
    public boolean penetrable;

    /** Which directions a unit is blocked by this prop. */
    public String blockDir;

    /** Returns a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    /**
     * Returns the prop configuration for the specified prop type.
     */
    public static PropConfig getConfig (String type)
    {
        ensurePropsRegistered();
        return _types.get(type);
    }

    /**
     * Returns a collection containing all registered prop configurations.
     */
    public static Collection<PropConfig> getConfigs ()
    {
        ensurePropsRegistered();
        return _types.values();
    }

    /**
     * Returns an array of configurations for all prop types
     * accessible in the specified town.
     */
    public static PropConfig[] getTownProps (String townId)
    {
        ensurePropsRegistered();
        return _townMap.get(townId);
    }

    public static void main (String[] args)
    {
        ensurePropsRegistered();
        for (PropConfig config : _types.values()) {
            System.err.println("" + config);
        }
    }

    protected static void ensurePropsRegistered ()
    {
        if (_types == null) {
            _types = new HashMap<String,PropConfig>();
            _townMap = new HashMap<String,PropConfig[]>();
            String[] props = BangUtil.townResourceToStrings("rsrc/props/TOWN/props.txt");
            for (int ii = 0; ii < props.length; ii++) {
                registerProp(props[ii]);
            }
        }
    }

    protected static void registerProp (String type)
    {
        // load up the properties file for this prop
        Properties props = BangUtil.resourceToProperties(
            "rsrc/props/" + type + "/prop.properties");

        // fill in a config instance from the properties file
        PropConfig config = new PropConfig();
        config.type = type;
        config.model = props.getProperty("model", type);
        config.variant = props.getProperty("variant");
        config.propClass = props.getProperty("class");
        config.scenario = props.getProperty("scenario");

        config.width = BangUtil.getIntProperty(type, props, "width", 1);
        config.length = BangUtil.getIntProperty(type, props, "length", 1);
        config.height = BangUtil.getFloatProperty(type, props, "height", 2f);

        config.tall = Boolean.parseBoolean(props.getProperty("tall"));
        config.passable = Boolean.parseBoolean(props.getProperty("passable"));
        config.passElev = BangUtil.getFloatProperty(type, props, "passElev",
                (config.passable ? 0f : config.height));
        config.penetrable = Boolean.parseBoolean(
            props.getProperty("penetrable",
                (config.width == 1 && config.length == 1) ? "true" : "false"));
        config.blockDir = props.getProperty("blockDir", "");

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

    protected static void mapTown (String town, PropConfig config)
    {
        PropConfig[] configs = _townMap.get(town);
        if (configs == null) {
            configs = new PropConfig[0];
        }
        PropConfig[] nconfigs = new PropConfig[configs.length+1];
        System.arraycopy(configs, 0, nconfigs, 0, configs.length);
        nconfigs[configs.length] = config;
        _townMap.put(town, nconfigs);
    }

    /** A mapping from prop type to its configuration. */
    protected static HashMap<String,PropConfig> _types;

    /** A mapping from town to all props accessible in that town. */
    protected static HashMap<String,PropConfig[]> _townMap;
}
