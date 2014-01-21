//
// $Id$

package com.threerings.bang.gang.server.persist;

/**
 * Contains information loaded from the database about a gang's outfit.
 */
public class GangOutfitRecord
{
    /** The gang using the outfit. */
    public int gangId;

    /** The configured article. */
    public String article;
    
    /** The encoded article colorizations. */
    public int zations;
    
    public GangOutfitRecord (int gangId, String article, int zations)
    {
        this.gangId = gangId;
        this.article = article;
        this.zations = zations;
    }
    
    public GangOutfitRecord ()
    {
    }
}
