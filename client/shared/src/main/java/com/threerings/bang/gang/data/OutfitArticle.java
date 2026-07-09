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

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.OutfitArticle.class, "article", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.OutfitArticle.class, "zations", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.OutfitArticle.class, "article", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.OutfitArticle.class, "zations", this, out);
    }
}
