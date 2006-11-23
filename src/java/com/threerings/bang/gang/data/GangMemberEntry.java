//
// $Id$

package com.threerings.bang.gang.data;

import java.util.Date;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PlayerObject;

/**
 * Extends {@link PardnerEntry} with gang-related data.
 */
public class GangMemberEntry extends PardnerEntry
{
    /** The member's player id. */
    public int playerId;
    
    /** The member's gang rank. */
    public byte rank;
    
    /** The time at which the member joined the gang. */
    public long joined;

    /**
     * Constructor for entries loaded from the database.
     */
    public GangMemberEntry (
        int playerId, Handle handle, byte rank, Date joined, Date lastSession)
    {
        super(handle, lastSession);
        this.playerId = playerId;
        this.rank = rank;
        this.joined = joined.getTime();
    }
    
    /**
     * Constructor for online members.
     */
    public GangMemberEntry (PlayerObject player)
    {
        super(player.handle);
        playerId = player.playerId;
        rank = player.gangRank;
        joined = player.joinedGang;
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
    
    @Override // documentation inherited
    public String toString ()
    {
        return "[handle=" + handle + ", rank=" + rank + ", joined=" +
            joined + "]";
    }
}
