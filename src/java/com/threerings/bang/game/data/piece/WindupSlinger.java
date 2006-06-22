//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.effect.Effect;

/**
 * Handles some special custom behavior needed for the Windup Gunslinger.
 */
public class WindupSlinger extends Unit
{
    @Override // documentation inherited
    public ArrayList<Effect> tick (short tick, BangBoard board, Piece[] pieces)
    {
//         int odamage = damage;
//         damage = Math.min(100, damage + 5);
//         return (odamage != damage);
        // TODO: return an effect that damages the unit
        return null;
    }

    @Override // documentation inherited
    public boolean canActivateBonus (Bonus bonus)
    {
        return false;
    }
}
