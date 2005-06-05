//
// $Id$

package com.threerings.bang.data.piece;

/**
 * Handles the state and behavior of the dirigible piece.
 */
public class Dirigible extends Unit
    implements PlayerPiece
{
    @Override // documentation inherited
    public String getType ()
    {
        return "dirigible";
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
