//
// $Id$

package com.threerings.bang.server.persist;

import java.sql.Date;

import com.samskivert.util.StringUtil;

import com.threerings.bang.data.Handle;

/**
 * A record containing persistent information maintained about a Bang!
 * player.
 */
public class Player
{
    /** A flag indicating the player's gender. */
    public static final int IS_MALE_FLAG = 1 << 0;

    /** This player's unique identifier. */
    public int playerId;

    /** The authentication account name associated with this player. */
    public String accountName;

    /** The cowboy handle (in-game name) associated with this player. */
    public String handle;

    /** The amount of scrip this player holds. */
    public int scrip;

    /** The current avatar look selected by this player. */
    public String look;

    /** The avatar look selected by this player for their victory pose. */
    public String victoryLook;

    /** The avatar look selected by this player for their wanted poster. */
    public String wantedLook;

    /** The time at which this player was created (when they first starting
     * playing  this particular game). */
    public Date created;

    /** The number of sessions this player has played. */
    public int sessions;

    /** The cumulative number of minutes spent playing. */
    public int sessionMinutes;

    /** The time at which the player ended their last session. */
    public Date lastSession;

    /** Various one bit data (gender, etc.). */
    public int flags;

    /** A blank constructor used when loading records from the database. */
    public Player ()
    {
    }

    /** Constructs a blank player record for the supplied account. */
    public Player (String accountName)
    {
        this.accountName = accountName;
        this.look = "";
    }

    /** Returns true if the specified flag is set. */
    public boolean isSet (int flag)
    {
        return (flags & flag) == flag;
    }

    /** Returns our handle as a proper {@link Handle} instance. */
    public Handle getHandle ()
    {
        return new Handle(handle);
    }
    
    /** Generates a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
