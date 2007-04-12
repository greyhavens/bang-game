//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.Handle;

/**
 * Contains summary information on a back parlor room.
 */
public class ParlorInfo extends SimpleStreamableObject
    implements DSet.Entry
{
    /** Indicates the type of this parlor. */
    public enum Type { SOCIAL, RECRUITING, NORMAL, PARDNERS_ONLY, PASSWORD };

    /** The player that created the parlor. */
    public Handle creator;

    /** The type of this parlor. */
    public Type type;

    /** The number of occupants in this parlor. */
    public int occupants;

    /** If this parlor has matched games. */
    public boolean matched;

    /** If this is a server parlor. */
    public boolean server;

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        return creator;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        ParlorInfo oinfo = (ParlorInfo)other;
        return creator.equals(oinfo.creator) && type == oinfo.type && occupants == oinfo.occupants;
    }
}
