//
// $Id$

package com.samskivert.bang.data.piece;

import com.samskivert.bang.client.sprite.PieceSprite;
import com.samskivert.bang.client.sprite.UnitSprite;

/**
 * Handles the state and behavior of the tank piece.
 */
public class Tank extends Piece
    implements PlayerPiece
{
    /** A tank can fire at a target up to two squares away. */
    public static final int FIRE_DISTANCE = 2;

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new UnitSprite("tank");
    }

    @Override // documentation inherited
    public int getMoveDistance ()
    {
        return 5;
    }

    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        if (target instanceof Tank) {
            return 25;
        } else if (target instanceof Chopper) {
            return 25;
        } else if (target instanceof Artillery) {
            return 25;
        } else if (target instanceof Marine) {
            return 50;
        } else {
            return super.computeDamage(target);
        }
    }
}
