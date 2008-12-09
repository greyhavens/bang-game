//
// $Id$

package com.threerings.bang.gang.data;

import java.util.Date;

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

    /** On the server, the time that the gang last played a competition game. */
    public transient long lastPlayed;

    public GangEntry (Handle name)
    {
        this(name, new Date());
    }

    public GangEntry (Handle name, Date lastPlayed)
    {
        this.name = name;
        this.lastPlayed = lastPlayed.getTime();
    }

    public GangEntry ()
    {
    }

    // documentation inherited from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return name;
    }
}
