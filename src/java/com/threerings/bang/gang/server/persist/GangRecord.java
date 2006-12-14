//
// $Id$

package com.threerings.bang.gang.server.persist;

import java.sql.Timestamp;
import java.util.ArrayList;

import com.threerings.bang.data.Handle;
import com.threerings.bang.gang.data.GangMemberEntry;

/**
 * Contains information loaded from the database about a gang
 */
public class GangRecord
{
    /** The gang's unique identifier. */
    public int gangId;

    /** The name of the gang. */
    public String name;

    /** The date upon which the gang was founded. */
    public Timestamp founded;

    /** The gang's statement. */
    public String statement;
    
    /** The gang's home page. */
    public String url;
    
    /** The gang's accumulated notoriety points. */
    public int notoriety;

    /** The amount of scrip in the gang's coffers. */
    public int scrip;

    /** The number of coins in the gang's coffers. */
    public int coins;

    /** The encoded brand. */
    public byte[] brand;

    /** The encoded outfit. */
    public byte[] outfit;

    /** The members of the gang. */
    public transient ArrayList<GangMemberEntry> members;

    /** Used when creating new gangs. */
    public GangRecord (String name)
    {
        this.name = name;
        members = new ArrayList<GangMemberEntry>();
    }

    /** Used when forming queries. */
    public GangRecord (int gangId)
    {
        this.gangId = gangId;
    }

    /** Used when loading records from the database. */
    public GangRecord ()
    {
    }

    /** Returns the gang name as a {@link Handle}. */
    public Handle getName ()
    {
        return new Handle(name);
    }

    /** Returns a string representation of this instance. */
    public String toString ()
    {
        return "[gangId=" + gangId + ", name=" + name + ", founded=" +
            founded + ", scrip=" + scrip + ", coins=" + coins + "]";
    }
}
