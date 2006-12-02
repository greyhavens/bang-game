//
// $Id$

package com.threerings.bang.bounty.data;

import com.threerings.presents.dobj.DSet;

/**
 * Contains information on a particular board.
 */
public class BoardInfo implements DSet.Entry
{
    /** The name of this board. */
    public String name;

    /** The number of players for which this board was designed. */
    public int players;

    /** The scenarios supported by this board. */
    public String[] scenarios;

    // from interface DSet.Entry
    public Comparable getKey ()
    {
        return name + "@" + players;
    }
}
