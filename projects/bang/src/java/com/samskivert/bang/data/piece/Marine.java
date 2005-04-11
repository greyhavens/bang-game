//
// $Id$

package com.samskivert.bang.data.piece;

import com.samskivert.bang.client.sprite.PieceSprite;
import com.samskivert.bang.client.sprite.UnitSprite;

/**
 * Handles the state and behavior of the marine piece.
 */
public class Marine extends Piece
    implements PlayerPiece
{
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new UnitSprite("marine");
    }

    @Override // documentation inherited
    public int getSightDistance ()
    {
        return 9;
    }

    @Override // documentation inherited
    public int getMoveDistance ()
    {
        return 3;
    }

    @Override // documentation inherited
    public int getFireDistance ()
    {
        return 2;
    }

    @Override // documentation inherited
    public boolean removeWhenDead ()
    {
        return true;
    }

    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        if (target instanceof Tank) {
            return 13;
        } else if (target instanceof Chopper) {
            return 50;
        } else if (target instanceof Artillery) {
            return 25;
        } else if (target instanceof Marine) {
            return 20;
        } else {
            return super.computeDamage(target);
        }
    }
}
