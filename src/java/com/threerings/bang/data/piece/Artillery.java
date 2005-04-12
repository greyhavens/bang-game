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
        return super.validTarget(target) && !(target instanceof Dirigible);
    }

    @Override // documentation inherited
    public int getFireDistance ()
    {
        return 4;
    }

    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        if (target instanceof SteamGunman) {
            return 34;
        } else if (target instanceof Dirigible) {
            return 0;
        } else if (target instanceof Artillery) {
            return 34;
        } else if (target instanceof Gunslinger) {
            return 17;
        } else {
            return super.computeDamage(target);
        }
    }
}
