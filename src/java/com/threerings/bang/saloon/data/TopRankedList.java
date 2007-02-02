//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.AvatarInfo;
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
    public AvatarInfo topDogSnapshot;

    // documentation inherited from interface DSet.Key
    public Comparable getKey ()
    {
        return criterion;
    }
}
