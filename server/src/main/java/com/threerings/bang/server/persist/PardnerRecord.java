//
// $Id$

package com.threerings.bang.server.persist;

import java.sql.Date;

import com.threerings.bang.data.Handle;

/**
 * Contains information loaded from the database about a pardnership.
 */
public class PardnerRecord
{
    /** The handle of the other player. */
    public Handle handle;

    /** The time of the player's last session. */
    public Date lastSession;

    /** If null the pardnership is active, if non-null the invitation message from the pardner. */
    public String message;

    /**
     * Creates a new pardner record.
     */
    public PardnerRecord (Handle handle, Date lastSession, String message)
    {
        this.handle = handle;
        this.lastSession = lastSession;
        this.message = message;
    }

    /**
     * Returns true if this is an active record, false if it's a pending invitation.
     */
    public boolean isActive ()
    {
        return (message == null);
    }

    /**
     * Returns a string representation of this instance.
     */
    public String toString ()
    {
        return "[handle=" + handle + ", last=" + lastSession + ", message=" + message + "]";
    }
}
