//
// $Id$

package com.threerings.bang.data.piece;

import com.threerings.bang.client.sprite.PieceSprite;
import com.threerings.bang.client.sprite.UnitSprite;

/**
 * Handles the state and behavior of the gun slinger piece.
 */
public class Gunslinger extends Piece
    implements PlayerPiece
{
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new UnitSprite("gunslinger");
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
        if (target instanceof SteamGunman) {
            return 13;
        } else if (target instanceof Dirigible) {
            return 50;
        } else if (target instanceof Artillery) {
            return 25;
        } else if (target instanceof Gunslinger) {
            return 20;
        } else {
            return super.computeDamage(target);
        }
    }
}
