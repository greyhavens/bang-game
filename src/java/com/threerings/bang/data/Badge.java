//
// $Id$

package com.threerings.bang.data;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.jmex.bui.BImage;
import com.jmex.bui.icon.ImageIcon;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.threerings.util.MessageBundle;

import com.threerings.bang.avatar.data.AvatarCodes;
import com.threerings.bang.game.data.card.Card;

import com.threerings.bang.util.BangUtil;
import com.threerings.bang.util.BasicContext;

import static com.threerings.bang.Log.log;

/**
 * Represents a badge earned by the player by meeting some specified
 * criterion.
 */
public class Badge extends Item
{
    /** Defines the various badge types. */
    public static enum Type
    {
        DAILY_HIGH_SCORER, // daily high scorer
        WEEKLY_HIGH_SCORER, // weekly
        MONTHLY_HIGH_SCORER, // monthly

        // games played badges
        GAMES_PLAYED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.GAMES_PLAYED) >= 5;
            }
        },
        GAMES_PLAYED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.GAMES_PLAYED) >= 50;
            }
        },
        GAMES_PLAYED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.GAMES_PLAYED) >= 500;
            }
        },
        GAMES_PLAYED_4 {
            public boolean qualifies (PlayerObject user) {
                return false;
                // return user.stats.getIntStat(Stat.Type.GAMES_PLAYED) >= 2000;
            }
        },
        GAMES_PLAYED_5 {
            public boolean qualifies (PlayerObject user) {
                return false;
                // return user.stats.getIntStat(Stat.Type.GAMES_PLAYED) >= 5000;
            }
        },

        // units killed badges
        UNITS_KILLED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.UNITS_KILLED) >= 50;
            }
        },
        UNITS_KILLED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.UNITS_KILLED) >= 500;
            }
        },
        UNITS_KILLED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.UNITS_KILLED) >= 5000;
            }
        },

        // units lost badges
        UNITS_LOST_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.UNITS_LOST) >= 100;
            }
        },
        UNITS_LOST_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.UNITS_LOST) >= 10000;
            }
        },

        // highest points badges
        HIGHEST_POINTS_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.HIGHEST_POINTS) >= 500;
            }
        },
        HIGHEST_POINTS_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.HIGHEST_POINTS) >= 1000;
            }
        },

        // consecutive kills badges
        CONSEC_KILLS_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CONSEC_KILLS) >= 5;
            }
        },
        CONSEC_KILLS_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CONSEC_KILLS) >= 10;
            }
        },
        CONSEC_KILLS_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CONSEC_KILLS) >= 15;
            }
        },

        // consecutive event badges (wins means first place, losses means
        // fourth place only, not non-first place or even last place )
        CONSEC_WINS_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CONSEC_WINS) >= 5;
            }
        },
        CONSEC_WINS_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CONSEC_WINS) >= 15;
            }
        },
        CONSEC_WINS_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CONSEC_WINS) >= 30;
            }
        },
        CONSEC_LOSSES_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CONSEC_LOSSES) >= 5;
            }
        },
        CONSEC_LOSSES_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CONSEC_LOSSES) >= 15;
            }
        },

        // shots fired badges
        SHOTS_FIRED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.SHOTS_FIRED) >= 1000;
            }
        },
        SHOTS_FIRED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.SHOTS_FIRED) >= 25000;
            }
        },

        // distance moved badges
        DISTANCE_MOVED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.DISTANCE_MOVED) >= 1000;
            }
        },
        DISTANCE_MOVED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.DISTANCE_MOVED) >= 50000;
            }
        },
        DISTANCE_MOVED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.DISTANCE_MOVED) >=
                    500000;
            }
        },

        // cards played badges
        CARDS_PLAYED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CARDS_PLAYED) >= 50;
            }
        },
        CARDS_PLAYED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CARDS_PLAYED) >= 500;
            }
        },
        CARDS_PLAYED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CARDS_PLAYED) >= 5000;
            }
        },

        // bonuses collected badges
        BONUSES_COLLECTED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.BONUSES_COLLECTED) >=
                    100;
            }
        },
        BONUSES_COLLECTED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(
                    Stat.Type.BONUSES_COLLECTED) >= 1000;
            }
        },
        BONUSES_COLLECTED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(
                    Stat.Type.BONUSES_COLLECTED) >= 10000;
            }
        },

        // cash earned badges
        CASH_EARNED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CASH_EARNED) >= 10000;
            }
        },
        CASH_EARNED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CASH_EARNED) >= 100000;
            }
        },
        CASH_EARNED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CASH_EARNED) >= 1000000;
            }
        },

        // nuggets claimed badges
        NUGGETS_CLAIMED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.NUGGETS_CLAIMED) >= 10;
            }
        },
        NUGGETS_CLAIMED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.NUGGETS_CLAIMED) >= 100;
            }
        },
        NUGGETS_CLAIMED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.NUGGETS_CLAIMED) >= 1000;
            }
        },
        NUGGETS_CLAIMED_4 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.NUGGETS_CLAIMED) >=
                    10000;
            }
        },
        NUGGETS_CLAIMED_5 {
            public boolean qualifies (PlayerObject user) {
                return false; // TODO: 10 nuggets in a round
            }
        },

        // cattle rustled badges
        CATTLE_RUSTLED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CATTLE_RUSTLED) >= 10;
            }
        },
        CATTLE_RUSTLED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CATTLE_RUSTLED) >= 100;
            }
        },
        CATTLE_RUSTLED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CATTLE_RUSTLED) >= 1000;
            }
        },
        CATTLE_RUSTLED_4 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CATTLE_RUSTLED) >= 10000;
            }
        },
        CATTLE_RUSTLED_5 {
            public boolean qualifies (PlayerObject user) {
                return false; // TODO: all cattle branded at same time
            }
        },

        // homesteads claimed badges
        STEADS_CLAIMED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.STEADS_CLAIMED) >= 10;
            }
        },
        STEADS_CLAIMED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.STEADS_CLAIMED) >= 50;
            }
        },
        STEADS_CLAIMED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.STEADS_CLAIMED) >= 500;
            }
        },
        STEADS_DESTROYED_1 {
            public boolean qualifies (PlayerObject user) {
                return false; // TODO: STEADS_DESTROYED >= 10
            }
        },
        STEADS_DESTROYED_2 {
            public boolean qualifies (PlayerObject user) {
                return false; // TODO: STEADS_DESTROYED >= 50
            }
        },

        // totems stacked badges
        TOTEMS_STACKED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.TOTEMS_STACKED) >= 10;
            }
        },
        TOTEMS_STACKED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.TOTEMS_STACKED) >= 100;
            }
        },
        TOTEMS_STACKED_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.TOTEMS_STACKED) >= 1000;
            }
        },
        TOTEMS_STACKED_4 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.TOTEMS_LARGE) >= 100;
            }
        },
        TOTEMS_STACKED_5 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.TOTEMS_CROWN) >= 100;
            }
        },

        // trees saved badges
        TREES_SAVED_1 {
            public boolean qualifies (PlayerObject user) {
                return false; // TODO: five perfect waves
            }
        },
        TREES_SAVED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.TREES_ELDER) >= 250;
            }
        },
        TREES_SAVED_3 {
            public boolean qualifies (PlayerObject user) {
                return false; // TODO: 20+ trees saved in one game
            }
        },
        TREES_SAVED_4 {
            public boolean qualifies (PlayerObject user) {
                return false; // TODO: player kills N hard robots in one round
            }
        },
        TREES_SAVED_5 {
            public boolean qualifies (PlayerObject user) {
                return false; // TODO: complete a wave at 10 saws difficulty
            }
        },

        // wendigo survival badges
        WENDIGO_SURVIVALS_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(
                    Stat.Type.WENDIGO_SURVIVALS) >= 30;
            }
        },
        WENDIGO_SURVIVALS_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(
                    Stat.Type.WENDIGO_SURVIVALS) >= 500;
            }
        },
        WENDIGO_SURVIVALS_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(
                    Stat.Type.WENDIGO_SURVIVALS) >= 5000;
            }
        },
        WENDIGO_SURVIVALS_4 {
            public boolean qualifies (PlayerObject user) {
                return false; // TODO: 100 talisman+safespot survivals
            }
        },
        WENDIGO_SURVIVALS_5 {
            public boolean qualifies (PlayerObject user) {
                return false; // TODO: 50 whole team survivals
            }
        },

        // frontier town unit usage badges
        CAVALRY_USER {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getMapValue(
                    Stat.Type.UNITS_USED, "frontier_town/cavalry") >= 100;
            }
        },
        TACTICIAN_USER {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getMapValue(
                    Stat.Type.UNITS_USED, "frontier_town/tactician") >= 100;
            }
        },
        CODGER_USER {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getMapValue(
                    Stat.Type.UNITS_USED, "frontier_town/codger") >= 100;
            }
        },
        FT_BIGSHOT_USER {
            public boolean qualifies (PlayerObject user) {
                return false; // TODO: 10 wins with each bigshot
            }
        },
        FT_ALLUNIT_USER {
            public boolean qualifies (PlayerObject user) {
                return checkUnitUsage(user.stats,
                    BangCodes.FRONTIER_TOWN, ALL_UNITS, 10);
            }
        },

        // indian trading post unit usage badges
        STORM_CALLER_USER {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getMapValue(
                    Stat.Type.UNITS_USED, "indian_post/stormcaller") >= 100;
            }
        },
        TRICKSTER_RAVEN_USER {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getMapValue(
                    Stat.Type.UNITS_USED, "indian_post/tricksterraven") >= 100;
            }
        },
        REVOLUTIONARY_USER {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getMapValue(
                    Stat.Type.UNITS_USED, "indian_post/revolutionary") >= 100;
            }
        },
        ITP_BIGSHOT_USER {
            public boolean qualifies (PlayerObject user) {
                return false; // TODO: 10 wins with each bigshot
            }
        },
        ITP_ALLUNIT_USER {
            public boolean qualifies (PlayerObject user) {
                return checkUnitUsage(user.stats,
                    BangCodes.INDIAN_POST, ALL_UNITS, 10);
            }
        },

        // social badges
        GAMES_HOSTED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.GAMES_HOSTED) >= 50;
            }
        },
        GAMES_HOSTED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.GAMES_HOSTED) >= 250;
            }
        },
        CHAT_SENT_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CHAT_SENT) >= 1000;
            }
        },
        CHAT_SENT_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CHAT_SENT) >= 5000;
            }
        },
        CHAT_SENT_3 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CHAT_SENT) >= 15000;
            }
        },
        CHAT_RECEIVED_1 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CHAT_RECEIVED) >= 1000;
            }
        },
        CHAT_RECEIVED_2 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.CHAT_RECEIVED) >= 5000;
            }
        },

        // avatar-related badges
        LOOKS_BOUGHT_1 {
            public boolean qualifies (PlayerObject user) {
                return user.looks.size() > 5;
            }
        },
        LOOKS_BOUGHT_2 {
            public boolean qualifies (PlayerObject user) {
                return user.looks.size() > 15;
            }
        },
        DUDS_BOUGHT_1 {
            public boolean qualifies (PlayerObject user) {
                return user.getDudsCount() >= 5;
            }
        },
        DUDS_BOUGHT_2 {
            public boolean qualifies (PlayerObject user) {
                return user.getDudsCount() >= 20;
            }
        },
        DUDS_BOUGHT_3 {
            public boolean qualifies (PlayerObject user) {
                return user.getDudsCount() >= 50;
            }
        },

        // wacky badges
        IRON_HORSE {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(
                    Stat.Type.SESSION_GAMES_PLAYED) >= 25;
            }
        },
        SAINT_NICK {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.MYSTERY_TWO) >= 1;
            }
        },
        NIGHT_OWL {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.LATE_NIGHTS) >= 5000;
            }
        },
        HIGH_NOON {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.MYSTERY_ONE) >= 1;
            }
        },
        NEW_SHERRIF_IN_TOWN,

        UNUSED;

        /** Returns a new blank stat instance of the specified type. */
        public Badge newBadge ()
        {
            return new Badge(code());
        }

        /** Overridden by each badge type to indicate whether the supplied
         * user qualifies for this badge. */
        public boolean qualifies (PlayerObject user) {
            return false;
        }

        /** Returns the translation key used by this badge. */
        public String key () {
            return "m.badge_" + Integer.toHexString(_code);
        }

        /** Returns the unique code for this badge type, which is a function of
         * its name. */
        public int code () {
            return _code;
        }

        Type () {
            // compute our unique code
            StringBuilder codestr = new StringBuilder();
            _code = BangUtil.crc32(name());

            if (_codeToType == null) {
                _codeToType = new HashIntMap<Type>();
            }
            if (_codeToType.containsKey(_code)) {
                log.warning("Badge type collision! " + this + " and " +
                            _codeToType.get(_code) + " both map to '" +
                            _code + "'.");
            } else {
                _codeToType.put(_code, this);
            }
        }

        protected int _code;
    };

    /** Defines the layout of the badge table. */
    public static Type[] LAYOUT =
    {
        // general series badges
        Type.GAMES_PLAYED_1, Type.GAMES_PLAYED_2, Type.GAMES_PLAYED_3,
        Type.GAMES_PLAYED_4, Type.GAMES_PLAYED_5,

        Type.UNITS_KILLED_1, Type.UNITS_KILLED_2, Type.UNITS_KILLED_3,
        null, null,

        Type.HIGHEST_POINTS_1, Type.HIGHEST_POINTS_2,
        Type.CONSEC_KILLS_1, Type.CONSEC_KILLS_2, Type.CONSEC_KILLS_3,

        Type.CONSEC_WINS_1, Type.CONSEC_WINS_2, Type.CONSEC_WINS_3,
        null, null,

        Type.SHOTS_FIRED_1, Type.SHOTS_FIRED_2,
        Type.DISTANCE_MOVED_1, Type.DISTANCE_MOVED_2, Type.DISTANCE_MOVED_3,

        Type.CARDS_PLAYED_1, Type.CARDS_PLAYED_2, Type.CARDS_PLAYED_3,
        null, null,

        Type.BONUSES_COLLECTED_1, Type.BONUSES_COLLECTED_2,
        Type.BONUSES_COLLECTED_3, null, null,

        Type.CASH_EARNED_1, Type.CASH_EARNED_2, Type.CASH_EARNED_3,
        Type.GAMES_HOSTED_1, Type.GAMES_HOSTED_2,

        Type.CHAT_SENT_1, Type.CHAT_SENT_2, Type.CHAT_SENT_3,
        Type.CHAT_RECEIVED_1, Type.CHAT_RECEIVED_2,

        Type.LOOKS_BOUGHT_1, Type.LOOKS_BOUGHT_2,
        Type.DUDS_BOUGHT_1, Type.DUDS_BOUGHT_2, Type.DUDS_BOUGHT_3,

//         DAILY_HIGH_SCORER, WEEKLY_HIGH_SCORER, MONTHLY_HIGH_SCORER,

        // frontier town badges
        Type.NUGGETS_CLAIMED_1, Type.NUGGETS_CLAIMED_2, Type.NUGGETS_CLAIMED_3,
        Type.NUGGETS_CLAIMED_4, Type.NUGGETS_CLAIMED_5,

        Type.CATTLE_RUSTLED_1, Type.CATTLE_RUSTLED_2, Type.CATTLE_RUSTLED_3,
        Type.CATTLE_RUSTLED_4, Type.CATTLE_RUSTLED_5,

        Type.STEADS_CLAIMED_1, Type.STEADS_CLAIMED_2, Type.STEADS_CLAIMED_3,
        Type.STEADS_DESTROYED_1, Type.STEADS_DESTROYED_2,

        Type.CAVALRY_USER, Type.TACTICIAN_USER, Type.CODGER_USER,
        Type.FT_BIGSHOT_USER, Type.FT_ALLUNIT_USER,

        // indian trading post badges
        Type.TOTEMS_STACKED_1, Type.TOTEMS_STACKED_2, Type.TOTEMS_STACKED_3,
        Type.TOTEMS_STACKED_4, Type.TOTEMS_STACKED_5,

        Type.TREES_SAVED_1, Type.TREES_SAVED_2, Type.TREES_SAVED_3,
        Type.TREES_SAVED_4, Type.TREES_SAVED_5,

        Type.WENDIGO_SURVIVALS_1, Type.WENDIGO_SURVIVALS_2,
        Type.WENDIGO_SURVIVALS_3, Type.WENDIGO_SURVIVALS_4,
        Type.WENDIGO_SURVIVALS_5,

        Type.STORM_CALLER_USER, Type.TRICKSTER_RAVEN_USER,
        Type.REVOLUTIONARY_USER, Type.ITP_BIGSHOT_USER, Type.ITP_ALLUNIT_USER,

        // you suck badges
        Type.UNITS_LOST_1, Type.UNITS_LOST_2, null,
        Type.CONSEC_LOSSES_1, Type.CONSEC_LOSSES_2,

        // general non-series (wacky) badges
        Type.IRON_HORSE, Type.SAINT_NICK, Type.NIGHT_OWL,
        Type.HIGH_NOON, Type.NEW_SHERRIF_IN_TOWN,
    };

    /**
     * Used to check certain badge bits.
     */
    public static void main (String[] args)
    {
        if (args.length > 0 && "check".indexOf(args[0]) != -1) {
            HashSet<Type> laidout = new HashSet<Type>();
            for (Type type : LAYOUT) {
                laidout.add(type);
            }
            for (Type type : Type.values()) {
                if (!laidout.contains(type) && type != Type.UNUSED) {
                    System.err.println("Not in layout: " + type);
                }
            }

        } else if (args.length > 0 && "dump".indexOf(args[0]) != -1) {
            for (Type type : Type.values()) {
                System.err.println(type + " = " + type.code());
            }

        } else if (args.length > 0 && "xlate".indexOf(args[0]) != -1) {
            for (Type type : Type.values()) {
                System.err.println(type.key() + " = " + type);
            }

        } else {
            System.err.println("Usage: Badge [check|dump|xlate]");
        }
    }

    /**
     * Determines whether this player qualifies for any new badges and returns
     * the first badge for which they qualify or null if they qualify for no
     * new badges.
     */
    public static Badge checkQualifies (PlayerObject user)
    {
        // first enumerate the badges they already hold
        _badgeCodes.clear();
        for (Item item : user.inventory) {
            if (item instanceof Badge) {
                _badgeCodes.add(((Badge)item).getCode());
            }
        }

        // now check each type in turn for qualification
        for (Type type : Type.values()) {
            if (_badgeCodes.contains(type.code())) {
                continue;
            }
            if (!type.qualifies(user)) {
                continue;
            }
            Badge badge = type.newBadge();
            badge.setOwnerId(user.playerId);
            return badge;
        }

        return null;
    }

    /** Creates a blank instance for serialization. */
    public Badge ()
    {
    }

    /**
     * Maps a {@link Type}'s code code back to a {@link Type} instance.
     */
    public static Type getType (int code)
    {
        return _codeToType.get(code);
    }

    /**
     * Returns the type of this badge.
     */
    public Type getType ()
    {
        return getType(_code);
    }

    /**
     * Returns the reward associated with this badge or null if the badge does
     * not confer any reward (other than the joy of collecting it).
     */
    public String getReward ()
    {
        if (_rewards.size() == 0) {
            registerRewards();
        }
        return _rewards.get(getType());
    }

    /**
     * Returns the integer code to which this badge's type maps.
     */
    public int getCode ()
    {
        return _code;
    }

    /**
     * Configures this badge to display a silhouette image and no name or
     * tooltip.  Used when displaying unearned badges in the badge list.
     */
    public void setSilhouette (boolean silhouette)
    {
        _silhouette = silhouette;
    }

    /**
     * Creates a new, unowned badge with the specified type code.
     */
    protected Badge (int code)
    {
        _code = code;
    }

    @Override // documentation inherited
    public String getName ()
    {
        if (_silhouette) {
            return null;
        }
        return MessageBundle.qualify(BangCodes.BADGE_MSGS, getType().key());
    }

    @Override // documentation inherited
    public String getTooltip ()
    {
        if (_silhouette) {
            return null;
        }
        String reward = getReward(), msg;
        if (reward == null) {
            msg = MessageBundle.compose("m.badge_icon_nil", getType().key());
        } else {
            msg = MessageBundle.compose(
                "m.badge_icon", getType().key(), reward);
        }
        return MessageBundle.qualify(BangCodes.BADGE_MSGS, msg);
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        String id = Integer.toHexString(getType().code());
        return "badges/" + id.substring(0,1) + "/" + id + ".png";
    }

    @Override // documentation inherited
    public ImageIcon createIcon (BasicContext ctx, String iconPath)
    {
        BImage bimage;
        if (_silhouette) {
            bimage = ctx.getImageCache().getSilhouetteBImage(iconPath, true);
            if (bimage == null) {
                bimage = ctx.getImageCache().getSilhouetteBImage(
                    "badges/noimage.png", true);
            }
        } else {
            bimage = ctx.getImageCache().getBImage(iconPath, true);
            if (bimage == null) {
                bimage = ctx.loadImage("badges/noimage.png");
            }
        }
        return new ImageIcon(bimage);
    }

    @Override // documentation inherited
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", type=").append(getType());
    }

    /**
     * Registers a message used to report the reward associated with obtaining
     * the specified type of badge.
     *
     * @param type the type of badge with which the reward is associated.
     * @param message the translatable string used to report the reward to the
     * player when they earn the badge.
     */
    protected static void registerReward (Type type, String message)
    {
        String old = _rewards.put(type, message);
        if (old != null) {
            log.warning("Badge registered for duplicate rewards " +
                        "[type=" + type + ", old=" + old +
                        ", new=" + message + "].");
        }
    }

    /** Used by unit usage badges. */
    protected static boolean checkUnitUsage (
        StatSet stats, String townId, EnumSet<UnitConfig.Rank> which,
        int usages)
    {
        for (UnitConfig cfg : UnitConfig.getTownUnits(townId, which)) {
            if (stats.getMapValue(Stat.Type.UNITS_USED, cfg.type) < usages) {
                return false;
            }
        }
        return true;
    }

    /**
     * Registers all badge awards. We have to do this on demand rather than in
     * a static initializer because of a twisty maze of already interdependent
     * static initializers.
     */
    protected static void registerRewards ()
    {
        String key = "m.hair_color_enabled", msg;
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_blue");
        registerReward(Type.UNITS_KILLED_2, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_green");
        registerReward(Type.SHOTS_FIRED_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_navyBlue");
        registerReward(Type.GAMES_PLAYED_3, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_orange");
        registerReward(Type.DISTANCE_MOVED_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_purple");
        registerReward(Type.CONSEC_WINS_2, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_violet");
        registerReward(Type.GAMES_PLAYED_2, MessageBundle.compose(key, msg));

        key = "m.eye_color_enabled";
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_violet");
        registerReward(Type.GAMES_PLAYED_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_orange");
        registerReward(Type.CASH_EARNED_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_purple");
        registerReward(Type.BONUSES_COLLECTED_2,
                       MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_red");
        registerReward(Type.UNITS_KILLED_3, MessageBundle.compose(key, msg));

        key = "m.duds_color_enabled";
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_black");
        registerReward(Type.CONSEC_WINS_3, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_leather");
        registerReward(Type.CARDS_PLAYED_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_orange");
        registerReward(Type.CARDS_PLAYED_2, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_pink");
        registerReward(Type.LOOKS_BOUGHT_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_violet");
        registerReward(Type.DUDS_BOUGHT_2, MessageBundle.compose(key, msg));

        key = "m.unit_enabled";
        msg = MessageBundle.qualify(BangCodes.UNITS_MSGS, "m.sharpshooter");
        registerReward(Type.CONSEC_WINS_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(BangCodes.UNITS_MSGS, "m.steamgunman");
        registerReward(Type.HIGHEST_POINTS_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(BangCodes.UNITS_MSGS, "m.dogsoldier");
        registerReward(Type.CONSEC_KILLS_2, MessageBundle.compose(key, msg));

        key = "m.cards_enabled";
        for (Card card : Card.getCards()) {
            Type qual = card.getQualifier();
            if (qual == null) {
                continue;
            }
            msg = MessageBundle.qualify(
                BangCodes.CARDS_MSGS, "m." + card.getType());
            registerReward(qual, MessageBundle.compose(key, msg));
        }
    }

    /** The unique code for the type of this badge. */
    protected int _code;

    /** Used when displaying unearned badges. */
    protected transient boolean _silhouette;

    /** The table mapping stat codes to enumerated types. */
    protected static HashIntMap<Type> _codeToType;

    /** Trigger the loading of the enum when we load this class. */
    protected static Type _trigger = Type.UNUSED;

    /** Used by {@link #checkQualifies}. */
    protected static ArrayIntSet _badgeCodes = new ArrayIntSet();

    /** Used to report rewards associated with badges. */
    protected static HashMap<Type,String> _rewards = new HashMap<Type,String>();

    /** Used by unit usage badges. */
    protected static final EnumSet<UnitConfig.Rank> BIGSHOT_UNITS =
        EnumSet.of(UnitConfig.Rank.BIGSHOT);

    /** Used by unit usage badges. */
    protected static final EnumSet<UnitConfig.Rank> ALL_UNITS =
        EnumSet.of(UnitConfig.Rank.BIGSHOT, UnitConfig.Rank.NORMAL);
}
