//
// $Id$

package com.threerings.bang.gang.server.persist;

import java.sql.Timestamp;
import java.util.ArrayList;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.OutfitArticle;

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

    /** The last time notoriety points were added. */
    public Timestamp lastPlayed;
    
    /** The amount of scrip in the gang's coffers. */
    public int scrip;

    /** The encoded brand. */
    public byte[] brand;

    /** The number of coins in the gang's coffers. */
    public transient int coins;
    
    /** The currently configured gang outfit. */
    public transient OutfitArticle[] outfit;
        
    /** The members of the gang. */
    public transient ArrayList<GangMemberEntry> members;

    /** The avatar of the most senior leader. */
    public transient AvatarInfo avatar;

    /** Used when creating new gangs. */
    public GangRecord (String name)
    {
        this.name = name;
        statement = "";
        url = "";
        outfit = new OutfitArticle[0];
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

    /** Returns the name used to identity the gang's entry in the coin database. */
    public String getCoinAccount ()
    {
        return "{" + name + "}"; 
    }
    
    /** Returns a string representation of this instance. */
    public String toString ()
    {
        return "[gangId=" + gangId + ", name=" + name + ", founded=" +
            founded + ", scrip=" + scrip + "]";
    }
}
