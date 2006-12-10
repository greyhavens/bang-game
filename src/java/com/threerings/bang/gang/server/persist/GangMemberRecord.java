//
// $Id$

package com.threerings.bang.gang.server.persist;

import java.sql.Timestamp;

/**
 * Contains information loaded from the database about a gang member.
 */
public class GangMemberRecord
{
    /** The member's player id. */
    public int playerId;
        
    /** The id of the gang to which the player belongs. */
    public int gangId;
        
    /** The player's rank in the gang. */
    public byte rank;
        
    /** The time at which the player joined the gang. */
    public Timestamp joined;
        
    /** The player's total contribution to the gang's notoriety. */
    public int notoriety;
        
    /** Used when adding new members. */
    public GangMemberRecord (int playerId, int gangId, byte rank)
    {
        this.playerId = playerId;
        this.gangId = gangId;
        this.rank = rank;
    }

    /** Used when forming queries. */
    public GangMemberRecord (int playerId)
    {
        this.playerId = playerId;
    }
        
    /** Used when loading records from the database. */
    public GangMemberRecord ()
    {
    }
}
