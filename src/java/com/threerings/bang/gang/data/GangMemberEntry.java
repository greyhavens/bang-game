//
// $Id$

package com.threerings.bang.gang.data;

import java.util.Date;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.AvatarInfo;
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

    /** The command order, for leaders (0 for the founder, 1 for the first member promoted to
     * leader, etc.) */
    public int commandOrder;

    /** The time at which the member joined the gang. */
    public long joined;

    /** The member's notoriety. */
    public int notoriety;

    /** The amount of scrip donated by the member. */
    public int scripDonated;

    /** The number of coins donated by the member. */
    public int coinsDonated;

    /** The index of the town that the member is logged into, or -1 if the member is offline. */
    public byte townIdx = -1;

    /** Whether or not the member has logged in recently. */
    public boolean wasActive;

    /** The member's avatar, if they're in the Hideout (on any server). */
    public AvatarInfo avatar;

    /** On the server, the time of the member's last session. */
    public transient long lastSession;

    /**
     * Constructor for entries loaded from the database.
     */
    public GangMemberEntry (
        Handle handle, int playerId, byte rank, int commandOrder, Date joined, int notoriety,
        int scripDonated, int coinsDonated, Date lastSession)
    {
        this.handle = handle;
        this.playerId = playerId;
        this.rank = rank;
        this.commandOrder = commandOrder;
        this.joined = joined.getTime();
        this.notoriety = notoriety;
        this.scripDonated = scripDonated;
        this.coinsDonated = coinsDonated;
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
        wasActive = (isOnline() ||
            (System.currentTimeMillis() - lastSession) < GangCodes.ACTIVITY_DELAY);
    }

    /**
     * Determines whether this member is active (is logged in now or has logged in recently).
     */
    public boolean isActive ()
    {
        return (isOnline() || wasActive);
    }

    /**
     * Determines whether this member is currently online.
     */
    public boolean isOnline ()
    {
        return (townIdx != -1);
    }

    /**
     * Checks whether the member is currently in the Hideout.
     */
    public boolean isInHideout ()
    {
        return (avatar != null);
    }

    /**
     * Determines whether the specified player can expel this member from the
     * gang, change his rank, etc., assuming that the player is in the same
     * gang.
     */
    public boolean canChangeStatus (PlayerObject player)
    {
        return canChangeStatus(player.gangRank, player.gangCommandOrder);
    }

    /**
     * Determines whether the specified member can expel this member from the
     * gang, change his rank, etc.
     */
    public boolean canChangeStatus (GangMemberEntry member)
    {
        return canChangeStatus(member.rank, member.commandOrder);
    }

    /**
     * Returns an array containing the amount of scrip and number of coins that must be reimbursed
     * to this member if he is expelled.
     */
    public int[] getDonationReimbursement ()
    {
        return new int[] {
            (scripDonated * GangCodes.DONATION_REIMBURSEMENT_PCT) / 100,
            (coinsDonated * GangCodes.DONATION_REIMBURSEMENT_PCT) / 100
        };
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

    protected boolean canChangeStatus (byte rank, int commandOrder)
    {
        return (rank == GangCodes.LEADER_RANK &&
            (this.rank != GangCodes.LEADER_RANK || commandOrder < this.commandOrder));
    }
}
