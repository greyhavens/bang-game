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

    /** The command order, for leaders (0 for the founder, 1 for the first member promoted to
     * leader, etc.) */
    public int commandOrder;

    /** The command level a leader has, which is used to restrict how soon they can perform
     * leader activites. */
    public int leaderLevel;

    /** The last time they performed a leader command. */
    public Timestamp lastLeaderCommand;

    /** The time at which the player joined the gang. */
    public Timestamp joined;

    /** The player's total contribution to the gang's notoriety. */
    public int notoriety;

    /** The amount of scrip donated by this member. */
    public int scripDonated;

    /** The number of coins donated by this member. */
    public int coinsDonated;

    /** The member title index. */
    public int title;

    /** Used when adding new members. */
    public GangMemberRecord (int playerId, int gangId, byte rank)
    {
        this.playerId = playerId;
        this.gangId = gangId;
        this.rank = rank;
    }

    /** Used when rolling back deletions. */
    public GangMemberRecord (
        int playerId, int gangId, byte rank, int commandOrder, int leaderLevel,
        long lastLeaderCommand, long joined, int notoriety,
        int scripDonated, int coinsDonated, int title)
    {
        this.playerId = playerId;
        this.gangId = gangId;
        this.rank = rank;
        this.commandOrder = commandOrder;
        this.leaderLevel = leaderLevel;
        this.lastLeaderCommand = new Timestamp(lastLeaderCommand);
        this.joined = new Timestamp(joined);
        this.notoriety = notoriety;
        this.scripDonated = scripDonated;
        this.coinsDonated = coinsDonated;
        this.title = title;
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
