//
// $Id$

package com.threerings.bang.data;

import java.io.IOException;
import java.util.EnumSet;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.presents.dobj.DSet;

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
        // meta statistics
        GAMES_PLAYED(new IntStat()),
        GAMES_WON(new IntStat()),
        GAME_TIME(new IntStat()),

        // stats accumulated during a game
        DISTANCE_MOVED(new IntStat()),
        SHOTS_FIRED(new IntStat()),
        DAMAGE_DEALT(new IntStat()),

        // stats accumulated during a game and persisted
        UNITS_KILLED(new IntStat()),
        UNITS_LOST(new IntStat()),
        BONUSES_COLLECTED(new IntStat()),
        CARDS_PLAYED(new IntStat()),
        CASH_EARNED(new IntStat()),

        CATTLE_RUSTLED(new IntStat()),
        NUGS_COLLECTED(new IntStat()),

        // stats derived from in-game statistics
        HIGHEST_EARNINGS(new IntStat()),
        MOST_KILLS(new IntStat()),

        UNUSED(new IntStat());

        /** Returns a new blank stat instance of the specified type. */
        public Stat newStat ()
        {
            return (Stat)_prototype.clone();
        }

        /** Returns the translation key used by this stat. */
        public String key () {
            return "m.stat_" + name().toLowerCase();
        }

        /** Returns the unique code for this stat which is a function of
         * its name. */
        public int code () {
            return _code;
        }

        /** Returns the unique string to which this stat's name was
         * reduced that must not collide with any other stat. */
        public String codeString () {
            return _codestr;
        }

        Type (Stat prototype) {
            // configure our prototype
            _prototype = prototype;
            _prototype._type = this;
            _prototype._modified = true;

            // compute our unique code
            StringBuffer codestr = new StringBuffer();
            _code = StringUtil.stringCode(name(), codestr);
            _codestr = codestr.toString();
        }

        protected Stat _prototype;
        protected int _code;
        protected String _codestr;
    };

    /**
     * Maps a {@link Type}'s code code back to a {@link Type} instance.
     */
    public static Type getType (int code)
    {
        // map our enumerated types back from their codes
        if (_codeToType == null) {
            _codeToType = new HashIntMap();
            for (Type type : EnumSet.allOf(Type.class)) {
                if (_codeToType.containsKey(type.code())) {
                    log.warning("Stat type collision! " + type + " and " +
                                _codeToType.get(type.code()) + " both map to '" +
                                type.codeString() + "'.");
                } else {
                    _codeToType.put(type.code(), type);
                }
            }
        }
        return (Type)_codeToType.get(code);
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
     * Writes our custom streamable fields.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        if (_nondb) {
            out.writeInt(_type.code());
        }
        out.defaultWriteObject();
    }

    /**
     * Reads our custom streamable fields.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        if (_nondb) {
            _type = getType(in.readInt());
        }
        in.defaultReadObject();
    }

    /**
     * Serializes this instance for storage in the item database.
     */
    public void persistTo (ObjectOutputStream out)
        throws IOException
    {
        _nondb = false;
        try {
            out.writeBareObject(this);
        } finally {
            _nondb = true;
        }
    }

    /**
     * Unserializes this item from data obtained from the item database.
     */
    public void unpersistFrom (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        _nondb = false;
        try {
            in.readBareObject(this);
        } finally {
            _nondb = true;
        }
    }

    /**
     * Generates a string representation of this instance.
     */
    public String toString ()
    {
        StringBuffer buf = new StringBuffer(_type.name().toLowerCase());
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
    public Comparable getKey ()
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

    /** Used when serializing this instance for storage in the database. */
    protected transient boolean _nondb = true;

    /** Indicates whether or not this statistic has been modified since it
     * was loaded from the database. */
    protected transient boolean _modified;

    /** The table mapping stat codes to enumerated types. */
    protected static HashIntMap _codeToType;
}
