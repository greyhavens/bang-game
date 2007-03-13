//
// $Id$

package com.threerings.bang.data;

import java.io.IOException;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangUtil;

import static com.threerings.bang.Log.log;

/**
 * A base class for persistent statistics tracked on a per-player basis
 * (some for a single game, others for all time).
 */
public abstract class Stat
    implements DSet.Entry, Cloneable
{
    /**
     * Defines the various per-player tracked statistics.
     */
    public static enum Type
    {
        // general statistics
        GAMES_PLAYED(new IntStat(), false, false),
        UNRANKED_GAMES_PLAYED(new IntStat(), false, false),
        GAMES_WON(new IntStat(), false, false),
        GAME_TIME(new MaxIntStat(), false, false),
        CONSEC_WINS(new IntStat(), false, false),
        CONSEC_LOSSES(new IntStat(), false, false),
        CONSEC_KILLS(new IntStat(), true, true), // most consec kills by one unit in one round
        LATE_NIGHTS(new IntStat(), false, false, true, true),
        TUTORIALS_COMPLETED(new ByteStringSetStat(), false, false, true, true),

        // transient (per-session) statistics
        SESSION_GAMES_PLAYED(new IntStat(), false, false, false, true),

        // stats accumulated during a game (not persisted)
        DAMAGE_DEALT(new MaxIntStat(), true, false, false, false),
        BONUS_POINTS(new MaxIntStat(), true, false, false, false),
        PACK_CARDS_PLAYED(new IntStat(), false, true, false, false),

        // stats accumulated during a game (persisted)
        UNITS_KILLED(new MaxIntStat(), true, true),
        UNITS_LOST(new MaxIntStat(), true, true),
        BONUSES_COLLECTED(new MaxIntStat(), true, false),
        CARDS_PLAYED(new MaxIntStat(), true, false),
        POINTS_EARNED(new IntStat(), true, false), // accumulated for whole game
        HIGHEST_POINTS(new IntStat(), false, true), // max points in single round
        CASH_EARNED(new MaxIntStat(), false, false),
        DISTANCE_MOVED(new MaxIntStat(), true, false),
        SHOTS_FIRED(new MaxIntStat(), true, false),
        UNITS_USED(new ByteByteStringMapStat(), false, false, true, true),
        BIGSHOT_WINS(new ByteByteStringMapStat(), false, false, true, true),

        PACK_CARD_WINS(new IntStat(), false, true), // brought cards into game and won
        BLUFF_CARD_WINS(new IntStat(), false, true), // brought 3 cards into game, played none, won

        CATTLE_RUSTLED(new MaxIntStat(), true, false),
        BRAND_POINTS(new MaxIntStat(), true, false),
        MOST_CATTLE(new IntStat(), true, false), // most cattle branded any time during round

        NUGGETS_CLAIMED(new MaxIntStat(), true, false),

        STEADS_CLAIMED(new MaxIntStat(), true, false),
        STEADS_DESTROYED(new MaxIntStat(), true, false),
        STEAD_POINTS(new MaxIntStat(), true, false),
        LONE_STEADER(new IntStat(), false, false), // all claimed steads are yours

        TOTEMS_SMALL(new MaxIntStat(), true, false),
        TOTEMS_MEDIUM(new MaxIntStat(), true, false),
        TOTEMS_LARGE(new MaxIntStat(), true, false),
        TOTEMS_CROWN(new MaxIntStat(), true, false),
        TOTEM_POINTS(new MaxIntStat(), true, false),

        WENDIGO_SURVIVALS(new MaxIntStat(), true, false),
        TALISMAN_POINTS(new MaxIntStat(), true, false),
        TALISMAN_SPOT_SURVIVALS(new MaxIntStat(), true, false),
        WHOLE_TEAM_SURVIVALS(new IntStat(), true, false),

        TREES_SAPLING(new MaxIntStat(), true, false),
        TREES_MATURE(new MaxIntStat(), true, false),
        TREES_ELDER(new MaxIntStat(), true, false),
        TREE_POINTS(new MaxIntStat(), true, false),
        WAVE_SCORES(new IntArrayStat(), false, false),
        WAVE_POINTS(new MaxIntStat(), true, false),
        HARD_ROBOT_KILLS(new MaxIntStat(), true, false),
        PERFECT_WAVES(new IntStat(), true, false),
        HIGHEST_SAWS(new IntStat(), false, false),

        // stats accumulated outside a game
        CHAT_SENT(new IntStat(), false, false),
        CHAT_RECEIVED(new IntStat(), false, false),
        GAMES_HOSTED(new IntStat(), false, false),

        // bounty related stats
        BOUNTIES_COMPLETED(new ShortStringSetStat(), false, false),
        BOUNTY_GAMES_COMPLETED(new ShortStringSetStat(), false, false),

        // free ticket related stats
        FREE_TICKETS(new ShortStringSetStat(), false, false),
        ACTIVATED_TICKETS(new ShortStringSetStat(), false, false),

        // stats that are meant to by mysterious
        MYSTERY_ONE(new IntStat(), false, false, false, true), // high noon logon
        MYSTERY_TWO(new IntStat(), false, false, false, true), // christmas morning game

        UNUSED(new IntStat(), false, false);

        /** Returns a new blank stat instance of the specified type. */
        public Stat newStat ()
        {
            return (Stat)_prototype.clone();
        }

        /** Returns the translation key used by this stat. */
        public String key () {
            return MessageBundle.qualify(BangCodes.STATS_MSGS,
                    "m.stat_" + StringUtil.toUSLowerCase(name()));
        }

        /** Returns the unique code for this stat which is a function of
         * its name. */
        public int code () {
            return _code;
        }

        /** Returns true if this stat is pertinent to bounty games. */
        public boolean isBounty ()
        {
            return _bounty;
        }

        /** Returns true if this stat should only be tracked in non-coop games. */
        public boolean isCompetitiveOnly ()
        {
            return _competitiveOnly;
        }

        /** Returns true if this stat is persisted between sessions. */
        public boolean isPersistent ()
        {
            return _persist;
        }

        /** Returns true if this stat is not shown in the stats display. */
        public boolean isHidden ()
        {
            return _hidden;
        }

        // most stats are persistent and not hidden
        Type (Stat prototype, boolean bounty, boolean compOnly) {
            this(prototype, bounty, compOnly, true, false);
        }

        Type (Stat prototype, boolean bounty, boolean compOnly, boolean persist, boolean hidden) {
            _bounty = bounty;
            _competitiveOnly = compOnly;
            _persist = persist;
            _hidden = hidden;

            // configure our prototype
            _prototype = prototype;
            _prototype._type = this;

            // compute our unique code
            _code = BangUtil.crc32(name());

            if (_codeToType.containsKey(_code)) {
                log.warning("Stat type collision! " + this + " and " +
                            _codeToType.get(_code) + " both map to '" +
                            _code + "'.");
            } else {
                _codeToType.put(_code, this);
            }
        }

        protected Stat _prototype;
        protected int _code;
        protected boolean _bounty, _competitiveOnly, _persist, _hidden;
    };

    /** Provides auxilliary information to statistics during the persisting
     * process. */
    public static interface AuxDataSource
    {
        /** Maps the specified string to a unique integer value. */
        public int getStringCode (Type type, String value);

        /** Maps the specified unique code back to its string value. */
        public String getCodeString (Type type, int code);
    }

    /**
     * Dumps the stat type to code mappings to stdout.
     */
    public static void main (String[] args)
    {
        for (Type type : Type.values()) {
            System.out.println(type + " = " + type.code());
        }
    }

    /**
     * Maps a {@link Type}'s code code back to a {@link Type} instance.
     */
    public static Type getType (int code)
    {
        return _codeToType.get(code);
    }

    /**
     * Returns the type of this statistic.
     */
    public Type getType ()
    {
        return _type;
    }

    /**
     * Returns the integer code to which this statistic's name maps.
     */
    public int getCode ()
    {
        return _type.code();
    }

    /**
     * Returns true if the supplied statistic has been modified since it
     * was loaded from the repository.
     */
    public boolean isModified ()
    {
        return _modified;
    }

    /**
     * Forces this stat to consider itself modified. Generally this is not
     * called but rather the derived class will update its modified state when
     * it is actually modified.
     */
    public void setModified (boolean modified)
    {
        _modified = modified;
    }

    /** Writes our custom streamable fields. */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.writeInt(_type.code());
        out.defaultWriteObject();
    }

    /** Reads our custom streamable fields. */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        _type = getType(in.readInt());
        in.defaultReadObject();
    }

    /**
     * Serializes this instance for storage in the item database. Derived
     * classes must override this method to implement persistence.
     */
    public abstract void persistTo (ObjectOutputStream out, AuxDataSource aux)
        throws IOException;

    /**
     * Unserializes this item from data obtained from the item database.
     * Derived classes must override this method to implement persistence.
     */
    public abstract void unpersistFrom (ObjectInputStream in, AuxDataSource aux)
        throws IOException, ClassNotFoundException;

    /**
     * Generates a string representation of this instance.
     */
    public String toString ()
    {
        StringBuffer buf = new StringBuffer(StringUtil.toUSLowerCase(_type.name()));
        buf.append("=");
        buf.append(valueToString());
        return buf.toString();
    }

    /**
     * Derived statistics must override this method and render their value
     * to a string. Used by {@link #toString} and to display the value in
     * game.
     */
    public abstract String valueToString ();

    // documentation inherited from DSet.Entry
    public String getKey ()
    {
        return _type.name();
    }

    // documentation inherited from Cloneable
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Clone failed", e);
        }
    }

    /** The type of the statistic in question. */
    protected transient Type _type;

    /** Indicates whether or not this statistic has been modified since it
     * was loaded from the database. */
    protected transient boolean _modified;

    /** The table mapping stat codes to enumerated types. */
    protected static HashIntMap<Type> _codeToType = new HashIntMap<Type>();

    /** Trigger the loading of the enum when we load this class. */
    protected static Type _trigger = Type.UNUSED;
}
