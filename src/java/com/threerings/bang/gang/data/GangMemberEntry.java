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
 * Extends {@link PardnerEntry} with gang-related data.
 */
public class GangMemberEntry extends SimpleStreamableObject
    implements DSet.Entry
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
    
    /**
     * Constructor for entries loaded from the database.
     */
    public GangMemberEntry (Handle handle, int playerId, byte rank, Date joined, int notoriety)
    {
        this.handle = handle;
        this.playerId = playerId;
        this.rank = rank;
        this.joined = joined.getTime();
        this.notoriety = notoriety;
    }
    
    /**
     * No-arg constructor for deserialization.
     */
    public GangMemberEntry ()
    {
    }

    /**
     * Determines whether the specified player can expel this member from the
     * gang, change his rank, etc., assuming that the player is in the same
     * gang.
     */
    public boolean canChangeStatus (PlayerObject player)
    {
        return (player.gangRank == GangCodes.LEADER_RANK &&
            (rank != GangCodes.LEADER_RANK || player.joinedGang < joined));
    }
    
    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        return handle;
    }
}
