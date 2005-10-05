//
// $Id$

package com.threerings.bang.server.persist;

import java.sql.Date;

/**
 * A record containing persistent information maintained about a Bang!
 * player.
 */
public class Player
{
    /** This player's unique identifier. */
    public int playerId;

    /** The authentication account name associated with this player. */
    public String accountName;

    /** The amount of scrip this player holds. */
    public int scrip;

    /** The current avatar look selected by this player. */
    public String look;

    /** The time at which this player was created (when they first starting
     * playing  this particular game). */
    public Date created;

    /** The number of sessions this player has played. */
    public int sessions;

    /** The cumulative number of minutes spent playing. */
    public int sessionMinutes;

    /** The time at which the player ended their last session. */
    public Date lastSession;

    /** A blank constructor used when loading records from the database. */
    public Player ()
    {
    }

    /** Constructs a blank player record for the supplied account. */
    public Player (String accountName)
    {
        this.accountName = accountName;
    }
}
