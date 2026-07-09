//
// $Id$

package com.threerings.bang.data;

import com.threerings.crowd.peer.data.CrowdClientInfo;

/**
 * Extends the peer server client info to contain an avatar.
 */
public class BangClientInfo extends CrowdClientInfo
{
    /** The player's unique identifier. */
    public int playerId;

    /** This player's avatar (used for pardner purposes). */
    public AvatarInfo avatar;

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        super.readObject(ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BangClientInfo.class, "playerId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.BangClientInfo.class, "avatar", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        super.writeObject(out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BangClientInfo.class, "playerId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.BangClientInfo.class, "avatar", this, out);
    }
}
