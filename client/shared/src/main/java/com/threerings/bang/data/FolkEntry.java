//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.dobj.DSet;

/**
 * Contains simple a player id, used to track friends and foes.
 */
public class FolkEntry
    implements DSet.Entry
{
    /** The id of the player in question. */
    public Integer playerId;

    public FolkEntry (int playerId)
    {
        this.playerId = playerId;
    }

    public FolkEntry ()
    {
    }

    // from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return playerId;
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.FolkEntry.class, "playerId", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.FolkEntry.class, "playerId", this, out);
    }
}
