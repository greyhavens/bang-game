//
// $Id$

package com.threerings.bang.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.jme.renderer.ColorRGBA;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.StringUtil;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.util.BangUtil;

import static com.threerings.bang.Log.*;

/**
 * Loads and manages terrain configuration information.
 */
public class TerrainConfig
{
    /** General categories under which terrain types fall. */
    public enum Category {
        NORMAL, ROAD
    };

    /** The terrain type. */
    public String type;

    /** The code used when encoding terrain in the {@link BangBoard}. */
    public int code;

    /** The general category of the terrain. */
    public Category category;

    /** The normal traversal cost of the terrain. */
    public int traversalCost;

    /** The terrain texture scale (the number of tile lengths the textures
     * cover). */
    public float scale;

    /** The color of the stuff that units kick up when they move over this
     * terrain.  The alpha value controls the "dustiness." */
    public ColorRGBA dustColor;

    /** Whether to use this texture in low graphics mode. */
    public boolean lowDetail;

    /** Whether to compress this texture if possible. */
    public boolean compress;

    /**
     * Returns the terrain configuration for the specified terrain type.
     */
    public static TerrainConfig getConfig (String type)
    {
        TerrainConfig config = _types.get(type);
        if (config == null) {
            log.warning("Requested unknown terrain config '" + type + "'!");
            // return some sort of default
            config = _types.get("dirt");
        }
        return config;
    }

    /**
     * Returns the terrain configuration for the specified terrain code.
     */
    public static TerrainConfig getConfig (int code)
    {
        TerrainConfig config = _codes.get(code);
        if (config == null) {
            log.warning("Requested unknown terrain config #" + code + "!");
            // return some sort of default
            config = _types.get("dirt");
        }
        return config;
    }

    /**
     * Returns a collection containing all registered terrain configurations.
     */
    public static Collection<TerrainConfig> getConfigs ()
    {
        return _types.values();
    }

    protected static void registerTerrain (String type, int code)
    {
        // load up the properties file for this terrain
        Properties props = BangUtil.resourceToProperties(
            "rsrc/terrain/" + type + "/terrain.properties");

        // fill in a config instance from the properties file
        TerrainConfig config = new TerrainConfig();
        config.type = type;
        config.code = code;
        config.category = Enum.valueOf(Category.class,
            StringUtil.toUSUpperCase(props.getProperty("category", "normal")));
        config.traversalCost = BangUtil.getIntProperty(
            type, props, "traversal", BangBoard.BASE_TRAVERSAL);
        config.scale = BangUtil.getFloatProperty(type, props, "scale", 1f);
        config.lowDetail = BangUtil.getBooleanProperty(type, props, "low_detail", true);
        config.compress = BangUtil.getBooleanProperty(type, props, "compress", true);

        // the default dust color is that of dirt
        float[] dcolor = StringUtil.parseFloatArray(
            props.getProperty("dust_color", "0.54, 0.45, 0.26, 0.8"));
        config.dustColor = new ColorRGBA(
            dcolor[0], dcolor[1], dcolor[2], dcolor[3]);

        // map the type and code to the config
        _types.put(type, config);
        _codes.put(code, config);
    }

    /** A mapping from terrain type to configuration. */
    protected static HashMap<String, TerrainConfig> _types =
        new HashMap<String, TerrainConfig>();

    /** A mapping from terrain code to configuration. */
    protected static HashIntMap<TerrainConfig> _codes =
        new HashIntMap<TerrainConfig>();

    static {
        // register our terrain types
        Properties props = BangUtil.resourceToProperties(
            "rsrc/terrain/codes.txt");
        for (Map.Entry<?, ?> entry : props.entrySet()) {
            registerTerrain((String)entry.getKey(), Integer.parseInt((String)entry.getValue()));
        }
    }
}
