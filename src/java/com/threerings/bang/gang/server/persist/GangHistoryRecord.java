//
// $Id$

package com.threerings.bang.gang.server.persist;

import java.sql.Timestamp;

/**
 * Contains information loaded from the database about a historical event.
 */
public class GangHistoryRecord
{
    /** The entry's unique identifier. */
    public int entryId;

    /** The gang to which the entry refers. */
    public int gangId;

    /** The time at which the event was recorded. */
    public Timestamp recorded;

    /** The event description. */
    public String description;

    /** Used when adding new events. */
    public GangHistoryRecord (int gangId, String description)
    {
        this.gangId = gangId;
        this.description = description;
    }

    /** Used when forming queries. */
    public GangHistoryRecord (int entryId)
    {
        this.entryId = entryId;
    }
}
