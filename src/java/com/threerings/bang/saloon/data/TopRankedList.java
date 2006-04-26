//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.Handle;

/**
 * Contains a list of top-ranked players according to some criterion.
 */
public class TopRankedList extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The criterion for this ranking. */
    public String criterion;

    /** The user ids of the players in rank order. */
    public int[] playerIds;

    /** The handles of the players in rank order. */
    public Handle[] players;

    /** A snapshot of the number one player. */
    public int[] topDogSnapshot;

    /**
     * Returns the top dog snapshot in a format embeddable in an in-game web
     * page.
     */
    public String getTopDogSnapshotURL ()
    {
        StringBuffer buf = new StringBuffer("avatar:///");
        int ll = (topDogSnapshot == null) ? 0 : topDogSnapshot.length;
        for (int ii = 0; ii < ll; ii++) {
            if (ii > 0) {
                buf.append(",");
            }
            buf.append(topDogSnapshot[ii]);
        }
        return buf.toString();
    }

    // documentation inherited from interface DSet.Key
    public Comparable getKey ()
    {
        return criterion;
    }
}
