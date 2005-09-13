//
// $Id$

package com.threerings.bang.data;

import java.util.EnumSet;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.StringUtil;

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
        // temporary names: these will be replaced with more descriptive monikers
        DAILY_HIGH_SCORER, // daily high scorer
        WEEKLY_HIGH_SCORER, // weekly
        MONTHLY_HIGH_SCORER, // monthly

        FIFTY_GAMES_PLAYED, // 50 games played
        FIVEC_GAMES_PLAYED, // 500
        ONEM_GAMES_PLAYED, // 1000
        FIVEM_GAMES_PLAYED, // 5000
        TENM_GAMES_PLAYED, // 10,000
        FIFTYM_GAMES_PLAYED, // 50,000

        FIVEC_UNITS_KILLED,  // 500 units killed
        FIVEM_UNITS_KILLED, // 5000
        FIFTYM_UNITS_KILLED, // 50,000

        ONEC_CATTLE_HERDED, // 100 cattle herded
        ONEM_CATTLE_HERDED, // 1000
        TENM_CATTLE_HERDED, // 10,000

        ONEC_NUGGETS_COLLECTED, // 100 nuggets collected
        ONEM_NUGGETS_COLLECTED, // 1000
        TENM_NUGGETS_COLLECTED, // 10,000

        FRTO_BIGSHOTS_USED, // all Frontier Town bigshots owned (used)
        INVI_BIGSHOTS_USED, // all Indian Village bigshots owned (used)
        BOTO_BIGSHOTS_USED, // all Boom Town bigshots owned (used)
        GHTO_BIGSHOTS_USED, // all Ghost Town bigshots owned (used)
        CIGO_BIGSHOTS_USED, // all City of Gold bigshots owned (used)

        FRTO_SPECIALS_USED, // all Frontier Town special units used
        INVI_SPECIALS_USED, // all Indian Village special units used
        BOTO_SPECIALS_USED, // all Boom Town special units used
        GHTO_SPECIALS_USED, // all Ghost Town special units used
        CIGO_SPECIALS_USED, // all City of Gold special units used

        UNUSED;

        /** Returns a new blank stat instance of the specified type. */
        public Badge newBadge ()
        {
            return new Badge(code());
        }

        /** Returns the translation key used by this badge. */
        public String key () {
            return "m.badge_" + name().toLowerCase();
        }

        /** Returns the unique code for this badge type, which is a
         * function of its name. */
        public int code () {
            return _code;
        }

        /** Returns the unique string to which this badge type's name was
         * reduced that must not collide with any other badge type. */
        public String codeString () {
            return _codestr;
        }

        Type () {
            // compute our unique code
            StringBuffer codestr = new StringBuffer();
            _code = StringUtil.stringCode(name(), codestr);
            _codestr = codestr.toString();

            if (_codeToType.containsKey(_code)) {
                log.warning("Badge type collision! " + this + " and " +
                            _codeToType.get(_code) + " both map to '" +
                            _codestr + "'.");
            } else {
                _codeToType.put(_code, this);
//                 log.info("Mapped " + this + " to " + _code +
//                          " (" + _codestr + ").");
            }
        }

        protected int _code;
        protected String _codestr;
    };

    public static void main (String[] args)
    {
        System.out.println("Code for 0: " + getType(0));
    }

    /** Creates a blank instance for serialization. */
    public Badge ()
    {
    }

    /**
     * Maps a {@Type}'s code code back to a {@link Type} instance.
     */
    public static Type getType (int code)
    {
        return (Type)_codeToType.get(code);
    }

    /**
     * Returns the type of this badge.
     */
    public Type getType ()
    {
        return getType(_code);
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

    /** The unique code for the type of this badge. */
    protected int _code;

    /** The table mapping stat codes to enumerated types. */
    protected static HashIntMap _codeToType = new HashIntMap();

    /** Trigger the loading of the enum when we load this class. */
    protected static Type _trigger = Type.UNUSED;
}
