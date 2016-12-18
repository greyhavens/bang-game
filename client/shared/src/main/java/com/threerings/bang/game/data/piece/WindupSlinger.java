//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;
import java.util.List;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;

/**
 * Handles some special custom behavior needed for the Windup Gunslinger.
 */
public class WindupSlinger extends Unit
{
    @Override // documentation inherited
    public ArrayList<Effect> tick (short tick, BangObject bangobj, List<Piece> pieces)
    {
//         int odamage = damage;
//         damage = Math.min(100, damage + 5);
//         return (odamage != damage);
        // TODO: return an effect that damages the unit
        return null;
    }

    @Override // documentation inherited
    public boolean canActivateBonus (BangObject bangobj, Bonus bonus)
    {
        return false;
    }
}
