//
// $Id$

package com.threerings.bang.data.piece;

/**
 * Handles the state and behavior of the gun slinger piece.
 */
public class Gunslinger extends Unit
    implements PlayerPiece
{
    @Override // documentation inherited
    public String getType ()
    {
        return "gunslinger";
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
            return 34;
        } else if (target instanceof Artillery) {
            return 34;
        } else if (target instanceof Gunslinger) {
            return 25;
        } else {
            return super.computeDamage(target);
        }
    }
}
