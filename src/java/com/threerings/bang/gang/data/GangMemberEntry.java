//
// $Id$

package com.threerings.bang.gang.data;

import java.util.Date;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PlayerObject;

/**
 * Contains information on a single gang member.
 */
public class GangMemberEntry extends SimpleStreamableObject
    implements Cloneable, DSet.Entry
{
    /** The member's handle. */
    public Handle handle;

    /** The member's player id. */
    public int playerId;

    /** The member's gang rank. */
    public byte rank;

    /** The time at which the member joined the gang. */
    public long joined;

    /** The member's notoriety. */
    public int notoriety;

    /** The index of the town that the member is logged into, or -1 if the member is offline. */
    public byte townIdx = -1;

    /** Whether or not the member has logged in recently. */
    public boolean wasActive;

    /** On the server, the time of the member's last session. */
    public transient long lastSession;

    /**
     * Constructor for entries loaded from the database.
     */
    public GangMemberEntry (
        Handle handle, int playerId, byte rank, Date joined, int notoriety, Date lastSession)
    {
        this.handle = handle;
        this.playerId = playerId;
        this.rank = rank;
        this.joined = joined.getTime();
        this.notoriety = notoriety;
        this.lastSession = lastSession.getTime();
        updateWasActive();
    }

    /**
     * No-arg constructor for deserialization.
     */
    public GangMemberEntry ()
    {
    }

    /**
     * Updates the {@link #wasActive} field based on the length of time since the member's last
     * session.
     */
    public void updateWasActive ()
    {
        wasActive = (townIdx != -1 ||
            (System.currentTimeMillis() - lastSession) < GangCodes.ACTIVITY_DELAY);
    }

    /**
     * Determines whether this member is active (is logged in now or has logged in recently).
     */
    public boolean isActive ()
    {
        return (townIdx != -1 || wasActive);
    }

    /**
     * Determines whether the specified player can expel this member from the
     * gang, change his rank, etc., assuming that the player is in the same
     * gang.
     */
    public boolean canChangeStatus (PlayerObject player)
    {
        return canChangeStatus(player.gangRank, player.joinedGang);
    }

    /**
     * Determines whether the specified menber can expel this member from the
     * gang, change his rank, etc.
     */
    public boolean canChangeStatus (GangMemberEntry member)
    {
        return canChangeStatus(member.rank, member.joined);
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

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        return handle;
    }

    protected boolean canChangeStatus (byte rank, long joined)
    {
        return (rank == GangCodes.LEADER_RANK &&
            (this.rank != GangCodes.LEADER_RANK || joined < this.joined));
    }
}
