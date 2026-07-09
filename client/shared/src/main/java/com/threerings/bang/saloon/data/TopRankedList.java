//
// $Id$

package com.threerings.bang.saloon.data;

import java.sql.Date;

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
    /** The periods that a top ranked list can be in. */
    public static final byte LIFETIME = 0;
    public static final byte THIS_WEEK = 1;
    public static final byte LAST_WEEK = 2;

    /** The criterion for this ranking. */
    public String criterion;

    /** The user ids of the players in rank order. */
    public int[] playerIds;

    /** The handles of the players in rank order. */
    public Handle[] players;

    /** A snapshot of the number one player. */
    public AvatarInfo topDogSnapshot;

    /** The period for these results. */
    public byte period;

    /** The week for this list. */
    public transient Date week;

    // documentation inherited from interface DSet.Key
    public Comparable<?> getKey ()
    {
        return criterion + period;
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.TopRankedList.class, "criterion", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.TopRankedList.class, "playerIds", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.TopRankedList.class, "players", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.TopRankedList.class, "topDogSnapshot", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.TopRankedList.class, "period", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.TopRankedList.class, "criterion", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.TopRankedList.class, "playerIds", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.TopRankedList.class, "players", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.TopRankedList.class, "topDogSnapshot", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.TopRankedList.class, "period", this, out);
    }
}
