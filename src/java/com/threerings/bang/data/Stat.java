//
// $Id$

package com.threerings.bang.data;

import java.io.IOException;
import java.util.EnumSet;
import java.util.logging.Level;

import com.samskivert.util.HashIntMap;

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
     * Defines the various per-player tracked statistics. <em>NOTE:</em>
     * old stats must NEVER be removed nor may their order be changed.
     */
    public static enum Type
    {
        // accumulating: total number of games played
        GAMES_PLAYED(new IntStat()),

        // accumulating: total number of games won
        GAMES_WON(new IntStat()),

        // accumulating: total number of minutes spent in games
        GAME_TIME(new IntStat()),

        // accumulating: total cash earned
        CASH_EARNED(new IntStat()),

        // accumulating: total points of damage dealt
        DAMAGE_DEALT(new IntStat()),

        // accumulating: total number of bonuses collected
        BONUSES_COLLECTED(new IntStat()),

        // accumulating: total number of cards played
        CARDS_PLAYED(new IntStat()),

        // maximum: highest earnings in a single game
        HIGHEST_EARNINGS(new IntStat()),

        // maximum: highest number of kills in a single game
        MOST_KILLS(new IntStat());

        /** Returns a new blank stat instance of the specified type. */
        public Stat newStat ()
        {
            return (Stat)_prototype.clone();
        }

        Type (Stat prototype) {
            _prototype = prototype;
            _prototype._type = this;
            _prototype._modified = true;
        }

        protected Stat _prototype;
    };

    /**
     * Maps a {@Type}'s ordinal code back to a {@link Type} instance.
     */
    public static Type getType (int code)
    {
        // map our enumerated types back from their ordinal codes
        if (_codeToType == null) {
            _codeToType = new HashIntMap();
            for (Type type : EnumSet.allOf(Type.class)) {
                _codeToType.put(type.ordinal(), type);
            }
        }
        return (Type)_codeToType.get(code);
    }

    /**
     * Returns the name of this statistic.
     */
    public String getName ()
    {
        return _type.name().toLowerCase().intern();
    }

    /**
     * Returns the integer code to which this statistic's name maps.
     */
    public int getCode ()
    {
        return _type.ordinal();
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
            out.writeInt(_type.ordinal());
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
        StringBuffer buf = new StringBuffer(getName());
        buf.append("=");
        valueToString(buf);
        return buf.toString();
    }

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

    /**
     * Derived statistics must override this method and render their value
     * to a string. Used by {@link #toString}.
     */
    protected abstract void valueToString (StringBuffer buf);

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
