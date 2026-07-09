//
// $Id$

package com.threerings.bang.game.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.data.Article;
import com.threerings.bang.data.Badge;
import com.threerings.bang.data.Item;

/**
 * Used to record and report awards at the end of a game.
 */
public class Award extends SimpleStreamableObject
    implements Comparable<Award>
{
    /** The player's rank in the game. */
    public int rank = -1;

    /** The player's index in the game. */
    public int pidx;

    /** The amount of cash "taken home" by this player. */
    public int cashEarned;

    /** The number of aces earned for the player's gang. */
    public int acesEarned;

    /** The badge or clothing article earned by this player if any. */
    public Item item;

    /** Default constructor used during unserialization. */
    public Award ()
    {
    }

    // documentation inherited from interface Comparable
    public int compareTo (Award other)
    {
        return rank - other.rank;
    }

    /**
     * Returns a compact representation of this award.
     */
    public String toString ()
    {
        String value = rank + ":" + cashEarned + ":";
        if (item instanceof Badge) {
            value += ((Badge)item).getType();
        } else if (item instanceof Article) {
            value += ((Article)item).getArticleName();
        } else {
            value += "none";
        }
        return value;
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.Award.class, "rank", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.Award.class, "pidx", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.Award.class, "cashEarned", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.Award.class, "acesEarned", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.Award.class, "item", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.Award.class, "rank", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.Award.class, "pidx", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.Award.class, "cashEarned", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.Award.class, "acesEarned", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.Award.class, "item", this, out);
    }
}
