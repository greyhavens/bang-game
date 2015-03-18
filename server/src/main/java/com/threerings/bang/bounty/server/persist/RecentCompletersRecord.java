//
// $Id$

package com.threerings.bang.bounty.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.samskivert.util.StringUtil;

import com.threerings.bang.bounty.data.RecentCompleters;

/**
 * Contains information on recent bounty completers.
 */
@Entity
public class RecentCompletersRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<RecentCompletersRecord> _R = RecentCompletersRecord.class;
    public static final ColumnExp<String> BOUNTY_ID = colexp(_R, "bountyId");
    public static final ColumnExp<String> TOWN_ID = colexp(_R, "townId");
    public static final ColumnExp<String> HANDLES = colexp(_R, "handles");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

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

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link RecentCompletersRecord}
     * with the supplied key values.
     */
    public static Key<RecentCompletersRecord> getKey (String bountyId)
    {
        return new Key<RecentCompletersRecord>(
                RecentCompletersRecord.class,
                new ColumnExp<?>[] { BOUNTY_ID },
                new Comparable<?>[] { bountyId });
    }
    // AUTO-GENERATED: METHODS END
}
