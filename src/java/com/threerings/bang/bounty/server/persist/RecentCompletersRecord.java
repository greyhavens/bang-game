//
// $Id$

package com.threerings.bang.bounty.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;

import com.samskivert.util.StringUtil;

import com.threerings.bang.bounty.data.RecentCompleters;

/**
 * Contains information on recent bounty completers.
 */
@Entity
public class RecentCompletersRecord extends PersistentRecord
{
    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** A column identifier for the {@link #townId} field. */
    public static final String TOWN_ID = "townId";

    /** The identifier of the bounty for which we track completers. */
    @Id public String bountyId;

    /** The town for which this bounty is applicable. */
    public String townId;

    /** The handles of the last ten completers. Tab separated. */
    public String handles;

    public RecentCompletersRecord ()
    {
    }

    public RecentCompletersRecord (String townId, RecentCompleters data)
    {
        bountyId = data.bountyId;
        this.townId = townId;
        handles = StringUtil.join(data.handles, "\t");
    }

    public RecentCompleters toRecentCompleters ()
    {
        RecentCompleters comp = new RecentCompleters();
        comp.bountyId = bountyId;
        comp.handles = StringUtil.split(handles, "\t");
        return comp;
    }

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
