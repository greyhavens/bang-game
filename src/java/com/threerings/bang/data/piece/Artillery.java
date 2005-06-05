//
// $Id$

package com.threerings.bang.data.piece;

/**
 * Handles the state and behavior of the artillery piece.
 */
public class Artillery extends Unit
    implements PlayerPiece
{
    @Override // documentation inherited
    public String getType ()
    {
        return "artillery";
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
            return 34;
        } else {
            return super.computeDamage(target);
        }
    }
}
