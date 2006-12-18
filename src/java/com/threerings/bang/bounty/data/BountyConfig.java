//
// $Id$

package com.threerings.bang.bounty.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

import com.samskivert.util.StringUtil;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.util.MessageBundle;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.util.BangUtil;

import static com.threerings.bang.Log.log;

/**
 * Describes the configuration of a particular bounty.
 */
public class BountyConfig extends SimpleStreamableObject
    implements Comparable<BountyConfig>
{
    /** Defines the two different types of bounties. */
    public enum Type { TOWN, MOST_WANTED };

    /** Defines the difficulty levels. */
    public enum Difficulty { EASY, MODERATE, CHALLENGING, MIND_BENDINGLY_HARD };

    /** Defines the mechanism that unlocks this bounty. */
    public enum LockType { NONE, BOUNTY, BADGE, LICENSE };

    /** Defines a reward for completing a bounty. */
    public static class Reward extends SimpleStreamableObject
    {
        /** Scrip payed when this bounty is completed. */
        public int scrip;
    }

    /** The town in which this bounty is available. */
    public String townId;

    /** Whether this is a Town or a Most Wanted bounty. */
    public Type type;

    /** Uniquely identifies this bounty. */
    public String ident;

    /** The mechanism that unlocks this bounty. */
    public LockType lock = LockType.NONE;

    /** The difficulty of this bounty. */
    public Difficulty difficulty;

    /** Whether or not the bounty games must be played in order. */
    public boolean inOrder = true;

    /** An optional character fingerprint for the outlaw. */
    public int[] outlawPrint;

    /** The names of our game definition files. */
    public ArrayList<String> games = new ArrayList<String>();

    /** The reward earned by completing this bounty. */
    public Reward reward;

    /**
     * Returns the bounty with the specified identifier.
     */
    public static BountyConfig getBounty (String ident)
    {
        ensureBountiesLoaded();
        return _configs.get(ident);
    }

    /**
     * Returns a sorted list of all bounties for the specified town and type.
     */
    public static ArrayList<BountyConfig> getTownBounties (String townId, Type type)
    {
        ensureBountiesLoaded();
        ArrayList<BountyConfig> matches = new ArrayList<BountyConfig>();
        for (BountyConfig config : _configs.values()) {
            if (config.type == type && config.townId.equals(townId)) {
                matches.add(config);
            }
        }
        Collections.sort(matches);
        return matches;
    }

    /**
     * Returns true if this bounty is available to the specified player.
     */
    public boolean isAvailable (PlayerObject user)
    {
        return true;
    }

    /**
     * Returns the (fully qualified) translatable string key for the specified bounty game.
     */
    public String getName (String game)
    {
        return MessageBundle.qualify(OfficeCodes.BOUNTY_MSGS, "m." + ident + "." + game);
    }

    /**
     * Returns the stat set key used to identify the supplied game (which must be one of this
     * bounty's games).
     */
    public String getStatKey (String game)
    {
        return ident + "." + game;
    }

    /**
     * Checks whether all of this bounty's games have been completed by the specified player.
     */
    public boolean isCompleted (PlayerObject user)
    {
        // if they have completed all the games, they're done!
        for (String game : games) {
            if (!user.stats.containsValue(Stat.Type.BOUNTY_GAMES_COMPLETED, getStatKey(game))) {
                return false;
            }
        }
        return true;
    }

    // from interface Comparable<BountyConfig>
    public int compareTo (BountyConfig other)
    {
        if (other.type != type) {
            return type.ordinal() - other.type.ordinal();
        }
        if (other.difficulty != difficulty) {
            return difficulty.ordinal() - other.difficulty.ordinal();
        }
        if (other.reward.scrip != reward.scrip) {
            return reward.scrip - other.reward.scrip;
        }
        if (other.games.size() != games.size()) {
            return games.size() - other.games.size();
        }
        return ident.compareTo(other.ident);
    }

    /**
     * Parses and registers a bounty from its property file definition.
     */
    protected static void registerBounty (String which)
    {
        // load up the properties file for this bounty
        Properties props = BangUtil.resourceToProperties(
            "rsrc/bounties/" + which + "/bounty.properties");

        // split the which into some metadata
        String[] bits = which.split("/");
        BountyConfig config = new BountyConfig();
        config.townId = bits[0];
        config.type = Type.valueOf(bits[1].toUpperCase());
        config.ident = bits[2];

        // parse the various bounty properties
        config.lock = BangUtil.getEnumProperty(which, props, "lock", LockType.NONE);
        config.difficulty = BangUtil.getEnumProperty(which, props, "difficulty", Difficulty.EASY);
        config.inOrder = BangUtil.getBooleanProperty(which, props, "in_order", config.inOrder);
        config.outlawPrint = StringUtil.parseIntArray(props.getProperty("outlaw_print", ""));

        for (String game : StringUtil.parseStringArray(props.getProperty("games", ""))) {
            config.games.add(game);
        }

        config.reward = new Reward();
        config.reward.scrip = BangUtil.getIntProperty(which, props, "reward_scrip", 0);

        // finally map it
        BountyConfig collide = _configs.put(config.ident, config);
        if (collide != null) {
            log.warning("Bounty name collision [which=" + which + ", old=" + collide + "].");
        }

        log.info("Registered " + config + ".");
    }

    protected static void ensureBountiesLoaded ()
    {
        if (_configs.size() == 0) {
            String[] configs = BangUtil.townResourceToStrings("rsrc/bounties/TOWN/bounties.txt");
            for (int ii = 0; ii < configs.length; ii++) {
                registerBounty(configs[ii]);
            }
        }
    }

    /** We cache all loaded bounty configs. */
    protected static HashMap<String,BountyConfig> _configs = new HashMap<String,BountyConfig>();
}
