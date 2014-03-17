//
// $Id$

package com.threerings.bang.bounty.data;

import com.samskivert.util.ListUtil;

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

    /**
     * Returns true if this board supports the specified player count and scenario.
     */
    public boolean matches (int players, String scenario)
    {
        return (this.players == players) &&
            ListUtil.indexOf(scenarios, scenario) != -1;
    }

    // from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return name + "@" + players;
    }

    @Override // from Object
    public String toString ()
    {
        return name;
    }

    @Override // from Object
    public int hashCode ()
    {
        return name.hashCode() ^ players;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof BoardInfo) {
            BoardInfo bother = (BoardInfo)other;
            return bother.name.equals(name) && bother.players == players;
        } else {
            return false;
        }
    }
}
