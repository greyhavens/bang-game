//
// $Id$

package com.threerings.bang.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.util.StreamableHashMap;
import com.threerings.presents.dobj.DSet;

/**
 * Contains data for a player's wanted poster.
 */
public class PosterInfo extends SimpleStreamableObject
    implements DSet.Entry, Cloneable
{
    public static final int BADGES = 4;

    /** The handle of this poster's player */
    public Handle handle;

    /** The name of the player's gang, or <code>null</code> for none. */
    public Handle gang;
    
    /** The player's rank in the gang. */
    public byte rank;
    
    /** This poster's player's avatar */
    public int[] avatar;

    /** The personal statement given by this poster's player */
    public String statement;

    /** The three favourite badges of this poster's player */
    public int[] badgeIds;

    /** The player's ranking in each scenario */
    public StreamableHashMap<String,Integer> rankings;

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }

    // from interface DSet.Entry
    public Comparable getKey ()
    {
        return handle;
    }
}
