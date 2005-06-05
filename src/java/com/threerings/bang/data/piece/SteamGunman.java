//
// $Id$

package com.threerings.bang.data.piece;

import com.threerings.bang.data.Terrain;

/**
 * Handles the state and behavior of the steam gunman piece.
 */
public class SteamGunman extends Unit
    implements PlayerPiece
{
    @Override // documentation inherited
    public String getType ()
    {
        return "steamgunman";
    }

    @Override // documentation inherited
    public int getMoveDistance ()
    {
        return 5;
    }

    @Override // documentation inherited
    public int traversalCost (Terrain terrain)
    {
        if (terrain == Terrain.ROAD) {
            return 5;
        } else {
            return 10;
        }
    }

    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        if (target instanceof SteamGunman) {
            return 25;
        } else if (target instanceof Dirigible) {
            return 25;
        } else if (target instanceof Artillery) {
            return 35;
        } else if (target instanceof Gunslinger) {
            return 50;
        } else {
            return super.computeDamage(target);
        }
    }
}
