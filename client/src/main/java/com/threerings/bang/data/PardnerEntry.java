
//
// $Id$

package com.threerings.bang.data;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.Handle;

/**
 * An entry in the list of pardners.
 */
public class PardnerEntry
    implements DSet.Entry, Comparable<PardnerEntry>, Cloneable
{
    /** The pardner is not logged in. */
    public static final byte OFFLINE = 0;

    /** The pardner is somewhere other than the saloon or a game. */
    public static final byte ONLINE = 1;

    /** The pardner is in the saloon interface. */
    public static final byte IN_SALOON = 2;

    /** The pardner is in a game. */
    public static final byte IN_GAME = 3;

    /** The pardner is doing a bounty. */
    public static final byte IN_BOUNTY = 4;

    /** The pardner is in a tutorial. */
    public static final byte IN_TUTORIAL = 5;

    /** The pardner's handle. */
    public Handle handle;

    /** The pardner's avatar. */
    public AvatarInfo avatar;

    /** The pardner's status ({@link #OFFLINE}, {@link #IN_SALOON}, etc). */
    public byte status;

    /** The place oid of the game your pardner is in. */
    public int gameOid;

    /**
     * No-arg constructor for deserialization.
     */
    public PardnerEntry ()
    {
    }

    /**
     * Constructor for online pardners.
     */
    public PardnerEntry (Handle handle)
    {
        this.handle = (handle == null) ? new Handle("") : handle;
    }

    /**
     * Constructor for offline pardners.
     */
    public PardnerEntry (Handle handle, Date lastSession)
    {
        this(handle);
        setLastSession(lastSession);
    }

    /**
     * Configures this player as online in the specified town.
     */
    public void setOnline (int townIndex)
    {
        status = (byte)((townIndex * 10) + ONLINE);
    }

    /**
     * Returns the town index in which this player is online, -1 if they are not online.
     */
    public int getTownIndex ()
    {
        return (status % 10 == ONLINE) ? (status/10) : -1;
    }

    /**
     * Determines whether this pardner is online.
     */
    public boolean isOnline ()
    {
        return status != OFFLINE;
    }

    /**
     * Determines whether this pardner is available for chat (i.e., online and
     * not in a game).
     */
    public boolean isAvailable ()
    {
        return (status % 10 == ONLINE) || status == IN_SALOON;
    }

    /**
     * Retrieves the date at which the pardner was last online.
     */
    public Date getLastSession ()
    {
        return new Date(_lastSessionEpoch + _lastSession * MILLISECONDS_PER_DAY);
    }

    /**
     * Sets the date at which the pardner was last online.
     */
    public void setLastSession (Date lastSession)
    {
        _lastSession = (short)((lastSession.getTime() - _lastSessionEpoch) / MILLISECONDS_PER_DAY);
    }

    // documentation inherited from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return handle;
    }

    // documentation inherited from interface Comparable
    public int compareTo (PardnerEntry oentry)
    {
        // sort online pardners above offline ones and available ones above unavailable ones
        if (isOnline() != oentry.isOnline()) {
            return isOnline() ? -1 : +1;

        } else if (isAvailable() != oentry.isAvailable()) {
            return isAvailable() ? -1 : +1;

        } else {
            return handle.compareTo(oentry.handle);
        }
    }

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null; // will not happen
        }
    }

    @Override // from Object
    public String toString ()
    {
        return handle + " (" + status + ", " + gameOid + ")";
    }

    /** For offline pardners, the date when the pardner last logged on, stored as the number of
     * days since midnight, 1/1/05 GMT. */
    protected short _lastSession;

    /** The time from which last session times are measured. */
    protected static long _lastSessionEpoch;
    static {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(2005, 0, 1, 0, 0, 0);
        _lastSessionEpoch = cal.getTimeInMillis();
    }

    /** The number of milliseconds in a day. */
    protected static final long MILLISECONDS_PER_DAY = 24L * 60 * 60 * 1000;
}
