//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.Handle;

/**
 * A gang listed in the Hideout directory.
 */
public class GangEntry extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The name of the gang. */
    public Handle name;

    public GangEntry (Handle name)
    {
        this.name = name;
    }

    public GangEntry ()
    {
    }

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        return name;
    }
}
