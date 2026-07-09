//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Song;

/**
 * Represents a full-length version of a single song for sale.
 */
public class SongGood extends Good
{
    /** All songs cost zero scrip. */
    public static final int SCRIP_COST = 0;

    /** All songs cost five coins. */
    public static final int COIN_COST = 5;

    /**
     * Creates a song good for the specified song.
     */
    public SongGood (String song, String townId)
    {
        super("song_" + song, townId, SCRIP_COST, COIN_COST, SONG_PRIORITY);
    }

    /**
     * A constructor used during unserialization.
     */
    public SongGood ()
    {
    }

    /**
     * Returns the identifier of the song being sold by this good.
     */
    public String getSong ()
    {
        return _type.substring("song_".length());
    }

    @Override // from Good
    public String getIconPath ()
    {
        return "goods/song.png";
    }

    @Override // from Good
    public String getTip ()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m.song_tip");
    }

    @Override // from Good
    public boolean isAvailable (PlayerObject user)
    {
        return !user.ownsSong(getSong());
    }

    @Override // documentation inherited
    public Item createItem (int playerId)
    {
        return new Song(playerId, getSong());
    }

    protected static final int SONG_PRIORITY = 0;

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_type", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_townId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_scripCost", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_coinCost", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.store.data.Good.class, "_priority", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_type", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_townId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_scripCost", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_coinCost", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.store.data.Good.class, "_priority", this, out);
    }
}
