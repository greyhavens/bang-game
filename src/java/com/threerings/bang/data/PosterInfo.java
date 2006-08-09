//
// $Id$

package com.threerings.bang.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

/**
 * Contains data for a player's wanted poster.
 */
public class PosterInfo extends SimpleStreamableObject
    implements DSet.Entry, Cloneable
{
    /** The handle of this poster's player */
    public Handle handle;

    /** This poster's player's avatar */
    public int[] avatar;

    /** The personal statement given by this poster's player */
    public String statement;

    /** The three favourite badges of this poster's player */
    public int[] badgeIds;

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
