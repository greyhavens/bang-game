//
// $Id$

package com.threerings.bang.data;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.threerings.util.MessageBundle;

import com.threerings.bang.avatar.data.AvatarCodes;

import com.threerings.bang.client.BadgeIcon;
import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.util.BangUtil;

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
                return user.stats.getIntStat(Stat.Type.GAMES_PLAYED) >= 2000;
            }
        },
        GAMES_PLAYED_5 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.GAMES_PLAYED) >= 5000;
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
        UNITS_KILLED_4 {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.UNITS_KILLED) >= 20000;
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

        // consecutive event badges
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

        // cards played badges
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
        LOOKS_BOUGHT_3 {
            public boolean qualifies (PlayerObject user) {
                return user.looks.size() > 30;
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

        // badges for using (owning) bigshots
        FRONTIER_BIGSHOTS_USED, // all Frontier Town bigshots owned (used)
        INDIAN_BIGSHOTS_USED, // all Indian Village bigshots owned (used)
        BOOM_BIGSHOTS_USED, // all Boom Town bigshots owned (used)
        GHOST_BIGSHOTS_USED, // all Ghost Town bigshots owned (used)
        GOLD_BIGSHOTS_USED, // all City of Gold bigshots owned (used)

        // badges for using special units
        FRONTIER_SPECIALS_USED, // all Frontier Town special units used
        INDIAN_SPECIALS_USED, // all Indian Village special units used
        BOOM_SPECIALS_USED, // all Boom Town special units used
        GHOST_SPECIALS_USED, // all Ghost Town special units used
        GOLD_SPECIALS_USED, // all City of Gold special units used

        // wacky badges
        IRON_HORSE,
        SAINT_NICK,
        NIGHT_OWL {
            public boolean qualifies (PlayerObject user) {
                return user.stats.getIntStat(Stat.Type.LATE_NIGHTS) >= 5000;
            }
        },
        HIGH_NOON,
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
            StringBuffer codestr = new StringBuffer();
            _code = BangUtil.crc32(name());

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

    public static void main (String[] args) {
        for (Type type : EnumSet.allOf(Type.class)) {
            System.err.println(type.key() + " = " + type);
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
        for (Iterator iter = user.inventory.iterator(); iter.hasNext(); ) {
            Object item = iter.next();
            if (item instanceof Badge) {
                _badgeCodes.add(((Badge)item).getCode());
            }
        }

        // now check each type in turn for qualification
        for (Type type : EnumSet.allOf(Type.class)) {
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
     * Creates a new, unowned badge with the specified type code.
     */
    protected Badge (int code)
    {
        _code = code;
    }

    @Override // documentation inherited
    public ItemIcon createIcon ()
    {
        return new BadgeIcon();
    }

    @Override // documentation inherited
    protected void toString (StringBuffer buf)
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
        registerReward(Type.CONSEC_WINS_1, MessageBundle.compose(key, msg));
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
        registerReward(Type.CONSEC_WINS_2, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_leather");
        registerReward(Type.CARDS_PLAYED_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_orange");
        registerReward(Type.CARDS_PLAYED_2, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_pink");
        registerReward(Type.LOOKS_BOUGHT_1, MessageBundle.compose(key, msg));
        msg = MessageBundle.qualify(AvatarCodes.AVATAR_MSGS, "m.col_violet");
        registerReward(Type.DUDS_BOUGHT_2, MessageBundle.compose(key, msg));
    }

    /** The unique code for the type of this badge. */
    protected int _code;

    /** The table mapping stat codes to enumerated types. */
    protected static HashIntMap<Type> _codeToType = new HashIntMap<Type>();

    /** Trigger the loading of the enum when we load this class. */
    protected static Type _trigger = Type.UNUSED;

    /** Used by {@link #checkQualifies}. */
    protected static ArrayIntSet _badgeCodes = new ArrayIntSet();

    /** Used to report rewards associated with badges. */
    protected static HashMap<Type,String> _rewards = new HashMap<Type,String>();
}
