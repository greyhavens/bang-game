//
// $Id$

package com.threerings.bang.data.piece;

import com.threerings.bang.client.sprite.PieceSprite;
import com.threerings.bang.client.sprite.UnitSprite;

/**
 * Handles the state and behavior of the dirigible piece.
 */
public class Dirigible extends Piece
    implements PlayerPiece
{
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new UnitSprite("dirigible");
    }

    @Override // documentation inherited
    public int getMoveDistance ()
    {
        return 3;
    }

    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        if (target instanceof SteamGunman) {
            return 34;
        } else if (target instanceof Dirigible) {
            return 25;
        } else if (target instanceof Artillery) {
            return 35;
        } else if (target instanceof Gunslinger) {
            return 17;
        } else {
            return super.computeDamage(target);
        }
    }
}
