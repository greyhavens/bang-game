//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.piece.Piece;

/**
 * Adds a to the board by having it fly out of a specified tile.
 */
public class AddSpawnedBonusEffect extends AddPieceEffect
{
    public static final String SPAWNED_BONUS = "spawned_bonus";

    public int x, y;

    public int wait;

    public boolean first;

    public AddSpawnedBonusEffect ()
    {
    }

    public AddSpawnedBonusEffect (Piece piece, int x, int y, int wait, boolean first)
    {
        super(piece, SPAWNED_BONUS);
        this.x = x;
        this.y = y;
        this.wait = wait;
        this.first = first;
    }

    @Override // documentation inherited
    public int[] getWaitPieces()
    {
        return new int[] { wait };
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        super.readObject(ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AddSpawnedBonusEffect.class, "x", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AddSpawnedBonusEffect.class, "y", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AddSpawnedBonusEffect.class, "wait", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AddSpawnedBonusEffect.class, "first", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        super.writeObject(out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AddSpawnedBonusEffect.class, "x", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AddSpawnedBonusEffect.class, "y", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AddSpawnedBonusEffect.class, "wait", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AddSpawnedBonusEffect.class, "first", this, out);
    }
}
