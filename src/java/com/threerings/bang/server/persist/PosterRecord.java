package com.threerings.bang.server.persist;

import com.samskivert.util.StringUtil;

/**
 * A record containing persistent information maintained about a
 * player's poster.
 */
public class PosterRecord
{
    /** The poster's player's unique identifier. */
    public int playerId;

    /** The statement text associated with this poster. */
    public String statement;

    /** A favourite badge of the poster's player */
    public int badge1;

    /** A favourite badge of the poster's player */
    public int badge2;

    /** A favourite badge of the poster's player */
    public int badge3;

    /** A favourite badge of the poster's player */
    public int badge4;

    /** Constructs a completely blank object, for when loading from database */
    public PosterRecord ()
    {
    }

    /** Constructs a blank poster record for the supplied player. */
    public PosterRecord (int playerId)
    {
        this.playerId = playerId;
        this.statement = "";
    }

    /** Generates a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
