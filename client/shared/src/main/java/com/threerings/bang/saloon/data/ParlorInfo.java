//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;

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

    /** The gangId for a recruiting parlor. */
    public int gangId;

    // documentation inherited from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return creator;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        ParlorInfo oinfo = (ParlorInfo)other;
        return creator.equals(oinfo.creator) && type == oinfo.type && occupants == oinfo.occupants;
    }

    /**
     * Returns true if this user can control the parlor.
     */
    public boolean powerUser (PlayerObject user)
    {
        // if this player is the creator, or an admin/support, let 'em in regardless
        return user.handle.equals(creator) || user.tokens.isSupport() ||
            (type == Type.RECRUITING && user.gangId == gangId && user.canRecruit());
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.ParlorInfo.class, "creator", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.ParlorInfo.class, "type", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.ParlorInfo.class, "occupants", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.ParlorInfo.class, "matched", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.ParlorInfo.class, "server", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.ParlorInfo.class, "gangId", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.ParlorInfo.class, "creator", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.ParlorInfo.class, "type", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.ParlorInfo.class, "occupants", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.ParlorInfo.class, "matched", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.ParlorInfo.class, "server", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.ParlorInfo.class, "gangId", this, out);
    }
}
