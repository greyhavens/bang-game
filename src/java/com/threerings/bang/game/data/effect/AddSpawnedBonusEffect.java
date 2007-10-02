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

    public AddSpawnedBonusEffect ()
    {
    }

    public AddSpawnedBonusEffect (Piece piece, int x, int y, int wait)
    {
        super(piece, SPAWNED_BONUS);
        this.x = x;
        this.y = y;
        this.wait = wait;
    }

    @Override // documentation inherited
    public int[] getWaitPieces()
    {
        return new int[] { wait };
    }
}
