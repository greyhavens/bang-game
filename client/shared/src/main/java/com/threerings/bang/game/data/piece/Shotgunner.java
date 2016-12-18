//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ShotEffect;

/**
 * Handles some special custom behavior needed for the Shotgunner.
 */
public class Shotgunner extends Unit
{
    @Override // documentation inherited
    public Effect[] collateralDamage (
        BangObject bangobj, Piece target, int damage)
    {
        // we damage pieces to the left and right of the one we're shooting
        ArrayList<Effect> shots = new ArrayList<Effect>();
        for (Piece p : bangobj.pieces) {
            if (target.getDistance(p) == 1 && validTarget(bangobj, p, false) &&
                // if it's not in line with us but is in line with the target,
                // then it's properly perpendicular
                (p.x != x && p.y != y) &&
                (p.x == target.x || p.y == target.y)) {
                // our collateral damage is half power
                ShotEffect shot = shoot(bangobj, p, 0.5f);
                shot.type = ShotEffect.COLLATERAL_DAMAGE;
                shots.add(shot);
            }
        }
        return (shots.size() == 0) ? null :
            shots.toArray(new Effect[shots.size()]);
    }
}
