//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.io.SimpleStreamableObject;

/**
 * Contains information on an element of a gang's outfit.
 */
public class OutfitArticle extends SimpleStreamableObject
{
    /** The name of the article. */
    public String article;
    
    /** The encoded article colorizations. */
    public int zations;
    
    /**
     * Constructor for entries loaded from the database.
     */
    public OutfitArticle (String article, int zations)
    {
        this.article = article;
        this.zations = zations;    
    }
    
    /**
     * No-arg constructor for deserialization.
     */
    public OutfitArticle ()
    {
    }
}
