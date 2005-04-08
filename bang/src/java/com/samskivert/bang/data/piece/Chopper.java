//
// $Id$

package com.samskivert.bang.data.piece;

import com.samskivert.bang.client.sprite.PieceSprite;
import com.samskivert.bang.client.sprite.UnitSprite;

/**
 * Handles the state and behavior of the chopper piece.
 */
public class Chopper extends Piece
    implements PlayerPiece
{
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new UnitSprite("chopper");
    }

    @Override // documentation inherited
    public int getMoveDistance ()
    {
        return 3;
    }

    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        if (target instanceof Tank) {
            return 34;
        } else if (target instanceof Chopper) {
            return 20;
        } else if (target instanceof Artillery) {
            return 25;
        } else if (target instanceof Marine) {
            return 17;
        } else {
            return super.computeDamage(target);
        }
    }
}
