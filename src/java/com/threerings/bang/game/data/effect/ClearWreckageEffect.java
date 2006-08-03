//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Clears all wreckage from the board.
 */
public class ClearWreckageEffect extends GlobalEffect
{
    @Override // documentation inherited
    public boolean validPiece (Piece piece)
    {
        return !piece.isAlive();
    }
    
    @Override // documentation inherited
    public Effect getEffect (Piece piece)
    {
        return new ClearPieceEffect(piece);
    }
}
