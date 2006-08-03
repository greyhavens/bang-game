//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.piece.Piece;

/**
 * Heals all the living pieces on the board.
 */
public class RepairAllEffect extends GlobalEffect
{
    @Override // documentation inherited
    public boolean validPiece (Piece piece)
    {
        return (piece.isAlive() && piece.damage > 0);
    }

    @Override // documentation inherited
    public Effect getEffect (Piece piece)
    {
        return new RepairEffect(piece.pieceId);
    }
}
