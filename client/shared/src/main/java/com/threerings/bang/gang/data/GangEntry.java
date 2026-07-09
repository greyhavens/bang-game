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

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.GangEntry.class, "name", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.GangEntry.class, "name", this, out);
    }
}
