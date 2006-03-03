//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.NuggetEffect;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Handles some special custom behavior needed for the Sharpshooter.
 */
public class Sharpshooter extends Unit
{
    @Override // documentation inherited
    public Effect[] collateralDamage (
        BangObject bangobj, Piece target, int damage)
    {
        if (!(target instanceof Unit)) {
            return null;
        }

        // the sharpshooter causes a unit holding a nugget to drop it; but not
        // if we're going to kill them, in which case they'd drop anyway
        Unit unit = (Unit)target;
        if (!unit.benuggeted || (unit.damage + damage >= 100)) {
            return null;
        }

        NuggetEffect effect = NuggetEffect.dropNugget(bangobj, unit);
        return (effect == null) ? null : new Effect[] { effect };
    }

    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        // we do 150% of the damage of the gunslinger in exchange for not
        // being able to shoot immediately next door
        return 15 * super.computeDamage(target) / 10;
    }
}
