//
// $Id$

package com.threerings.bang.data;

import java.util.HashMap;
import java.util.Properties;

import com.samskivert.util.StringUtil;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.util.BangUtil;

import static com.threerings.bang.Log.log;

/**
 * Loads and manages bonus configuration information.
 */
public class BonusConfig
{
    /** The name of this bonus type (ie. <code>repair</code>, etc.). */
    public String type;

    /** The bonus's base weight. */
    public int baseWeight;

    /** The bonus's affinity for players with high total damage. */
    public float damageAffinity;

    /** The bonus's affinity for players with low total power. */
    public float lowPowerAffinity;

    /** The bonus's affinity for players with many pieces. */
    public float manyPiecesAffinity;

    /** The bonus's affinity for players with few pieces. */
    public float fewPiecesAffinity;

    /** The bonus's affinity for the early game. */
    public float earlyGameAffinity;

    /** The bonus's affinity for the late game. */
    public float lateGameAffinity;

    /** The minimum point differential for this bonus to be available. */
    public int minPointDiff;

    /** If the bonus can be held by a unit. */
    public boolean holdable;

    /** If true, the bonus can only be activated by ground units. */
    public boolean groundOnly;

    /** If true, the bonus can only be activated by players (not by unaligned units). */
    public boolean playersOnly;

    /** If true, the bonus is hidden. */
    public boolean hidden;

    /** If specified, the bonus will only be spawned in this scenario. */
    public String scenario;

    /** The custom bonus class use for this bonus, if specified. */
    public String bonusClass;

    /** The effect class associated with this bonus. */
    public String effectClass;

    /** The card type associated with this bonus. */
    public String cardType;

    /** If the bonus should emit particles when it's on the board, the name of its particle
     * effect. */
    public String particleEffect;

    /** Returns a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    /**
     * Returns the bonus configuration for the specified bonus type.
     */
    public static BonusConfig getConfig (String type)
    {
        BonusConfig config = _types.get(type);
        if (config == null) {
            log.warning("Requested unknown bonus config", "type", type);
            Thread.dumpStack();
            // return something to avoid le booch
            config = _types.get("repair");
        }
        return config;
    }

    /**
     * Returns an array of configurations for all bonus types accessible in the specified town.
     */
    public static BonusConfig[] getTownBonuses (String townId)
    {
        return _townMap.get(townId);
    }

    public static void main (String[] args)
    {
        for (BonusConfig config : _types.values()) {
            System.err.println("" + config);
        }
    }

    /**
     * Computes the weight of this particular bonus.
     */
    public int getWeight (BangObject bangobj, double averagePower,
                          int averageDamage, int averagePieces, int pointDiff)
    {
        // bail out if the base weight is zero or it's not the required scenario
        if (baseWeight == 0 ||
            (scenario != null && !bangobj.scenario.getIdent().equals(scenario)) ||
            minPointDiff > pointDiff) {
            return 0;
        }

        // add contributions from each of our affinities
        int eweight = 0, ecount = 0;
        if (damageAffinity != 0) {
            eweight += Math.round(averageDamage * damageAffinity);
            ecount++;
        }

        int maxedAP = Math.min(10, averagePieces);
        if (manyPiecesAffinity != 0) {
            eweight += Math.round(10 * maxedAP * manyPiecesAffinity);
            ecount++;
        }
        if (fewPiecesAffinity != 0) {
            eweight += Math.round(10 * (11-maxedAP) * fewPiecesAffinity);
            ecount++;
        }

        if (lowPowerAffinity != 0) {
            double maxedPower = Math.min(1.0, averagePower);
            eweight += Math.round(100 * (1.0-maxedPower) * lowPowerAffinity);
            ecount++;
        }

        int scaledTurn = 100 * Math.min(bangobj.tick, 60) / 60;
        if (earlyGameAffinity != 0) {
            eweight += Math.round((100 - scaledTurn) * earlyGameAffinity);
            ecount++;
        }
        if (lateGameAffinity != 0) {
            eweight += Math.round(scaledTurn * lateGameAffinity);
            ecount++;
        }

        return baseWeight + (ecount > 0 ? (eweight / ecount) : 0);
    }

    protected static void registerBonus (String type)
    {
        // load up the properties file for this bonus
        Properties props = BangUtil.resourceToProperties(
            "rsrc/bonuses/" + type + "/bonus.properties");

        // fill in a config instance from the properties file
        BonusConfig config = new BonusConfig();
        config.type = type;
        config.bonusClass = props.getProperty("class");
        config.effectClass = BangUtil.requireProperty(type, props, "effect");
        config.cardType = props.getProperty("card");
        config.particleEffect = props.getProperty("particles");

        config.baseWeight = BangUtil.getIntProperty(type, props, "base_weight", 50);
        config.minPointDiff = BangUtil.getIntProperty(type, props, "min_point_diff", 0);
        config.damageAffinity = BangUtil.getFloatProperty(type, props, "damage_affinity", 0f);
        config.lowPowerAffinity = BangUtil.getFloatProperty(type, props, "low_power_affinity", 0f);

        config.manyPiecesAffinity = BangUtil.getFloatProperty(
            type, props, "many_pieces_affinity", 0f);
        config.fewPiecesAffinity = BangUtil.getFloatProperty(
            type, props, "few_pieces_affinity", 0f);

        config.earlyGameAffinity = BangUtil.getFloatProperty(
            type, props, "early_game_affinity", 0f);
        config.lateGameAffinity = BangUtil.getFloatProperty(type, props, "late_game_affinity", 0f);

        config.holdable = BangUtil.getBooleanProperty(type, props, "holdable", false);

        config.scenario = props.getProperty("scenario");

        config.groundOnly = BangUtil.getBooleanProperty(type, props, "ground_only", false);
        config.playersOnly = BangUtil.getBooleanProperty(type, props, "players_only", false);
        config.hidden = BangUtil.getBooleanProperty(type, props, "hidden", false);

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

    protected static void mapTown (String town, BonusConfig config)
    {
        BonusConfig[] configs = _townMap.get(town);
        if (configs == null) {
            configs = new BonusConfig[0];
        }
        BonusConfig[] nconfigs = new BonusConfig[configs.length+1];
        System.arraycopy(configs, 0, nconfigs, 0, configs.length);
        nconfigs[configs.length] = config;
        _townMap.put(town, nconfigs);
    }

    /** A mapping from bonus type to its configuration. */
    protected static HashMap<String,BonusConfig> _types = new HashMap<String,BonusConfig>();

    /** A mapping from town to all bonuses accessible in that town. */
    protected static HashMap<String,BonusConfig[]> _townMap = new HashMap<String,BonusConfig[]>();

    static {
        // register our bonuses
        String[] bonuses = BangUtil.townResourceToStrings("rsrc/bonuses/TOWN/bonuses.txt");
        for (int ii = 0; ii < bonuses.length; ii++) {
            registerBonus(bonuses[ii]);
        }
    }
}
