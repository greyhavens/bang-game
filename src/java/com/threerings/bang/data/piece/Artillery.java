//
// $Id$

package com.threerings.bang.data.piece;

import com.threerings.bang.client.sprite.PieceSprite;
import com.threerings.bang.client.sprite.UnitSprite;
import com.threerings.bang.data.piece.Piece;

/**
 * Handles the state and behavior of the artillery piece.
 */
public class Artillery extends Piece
    implements PlayerPiece
{
    /** A tank can fire at a target up to seven squares away. */
    public static final int FIRE_DISTANCE = 4;

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new UnitSprite("artillery");
    }

    @Override // documentation inherited
    public int getSightDistance ()
    {
        return 9;
    }

    @Override // documentation inherited
    public int getMoveDistance ()
    {
        return 2;
    }

    @Override // documentation inherited
    public boolean validTarget (Piece target)
    {
        return super.validTarget(target) && !(target instanceof Chopper);
    }

    @Override // documentation inherited
    public int getFireDistance ()
    {
        return FIRE_DISTANCE;
    }

    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        if (target instanceof Tank) {
            return 34;
        } else if (target instanceof Chopper) {
            return 0;
        } else if (target instanceof Artillery) {
            return 34;
        } else if (target instanceof Marine) {
            return 17;
        } else {
            return super.computeDamage(target);
        }
    }
}
