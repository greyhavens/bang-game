//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.BangBoard;

/**
 * Handles some special custom behavior needed for the Windup Gunslinger.
 */
public class WindupSlinger extends Unit
{
    @Override // documentation inherited
    public boolean tick (short tick, BangBoard board, Piece[] pieces)
    {
        int odamage = damage;
        damage = Math.min(100, damage + 5);
        return (odamage != damage);
    }

    @Override // documentation inherited
    public boolean canActivateBonus (Bonus bonus)
    {
        return false;
    }
}
