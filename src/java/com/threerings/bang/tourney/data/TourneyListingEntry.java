//
// $Id$

package com.threerings.bang.tourney.data;

import com.threerings.presents.dobj.DSet;

/**
 * Contains listing info about an active or pending tournament.
 */
public class TourneyListingEntry
    implements DSet.Entry
{
    /** Tourney Description. */
    public String desc;

    /** Minutes until start. */
    public int startsIn;

    /** The oid of the tourney we represent. */
    public int oid;

    /** The unique key for the tourney. */
    public Comparable<?> key;

    /**
     * Blank constructor.
     */
    public TourneyListingEntry ()
    {
    }

    /**
     * Creates a configured tourney listing entry.
     */
    public TourneyListingEntry (String desc, Comparable<?> key, int oid, int startsIn)
    {
        this.desc = desc;
        this.key = key;
        this.oid = oid;
        this.startsIn = startsIn;
    }

    // documentation inherited from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return key;
    }

    // documentation inherited
    public boolean equals (Object other)
    {
        TourneyListingEntry that = (TourneyListingEntry)other;
        return this.getKey().equals(that.getKey());
    }

    // documentation inherited
    public String toString ()
    {
        return desc;
    }
}
