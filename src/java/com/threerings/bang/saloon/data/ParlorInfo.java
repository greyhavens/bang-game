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
    /** The player that created the parlor. */
    public Handle creator;

    /** Whether or not this parlor is pardners only. */
    public boolean pardnersOnly;

    /** Whether or not this parlor is password protected. */
    public boolean passwordProtected;

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        return creator;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        ParlorInfo oinfo = (ParlorInfo)other;
        return creator.equals(oinfo.creator) &&
            pardnersOnly == oinfo.pardnersOnly &&
            passwordProtected == oinfo.passwordProtected;
    }
}
