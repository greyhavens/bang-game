//
// $Id$

package com.threerings.bang.game.data;

import java.io.IOException;
import java.util.ArrayList;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.PlayerObject;

/**
 * Used to record and report awards at the end of a game.
 */
public class Award extends SimpleStreamableObject
{
    /** The player in question (only valid on the server). */
    public transient PlayerObject player;

    /** The amount of cash "taken home" by this player. */
    public int cashEarned;

    /** The badges earned by this player. */
    public transient ArrayList<Badge> badges;

    /** Default constructor used during unserialization. */
    public Award ()
    {
    }

    /**
     * Creates an award record for the specified player.
     */
    public Award (PlayerObject player)
    {
        this.player = player;
        badges = new ArrayList<Badge>();
    }

    /** Provides custom serialization. */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();
        out.writeInt(badges.size());
        for (Badge badge : badges) {
            out.writeObject(badge);
        }
    }

    /** Provides custom serialization. */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        int count = in.readInt();
        badges = new ArrayList<Badge>(count);
        for (int ii = 0; ii < count; ii++) {
            badges.add((Badge)in.readObject());
        }
    }
}
