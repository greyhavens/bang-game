//
// $Id$

package com.threerings.bang.game.data.piece;

import java.util.ArrayList;
import java.util.Iterator;

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
        for (Iterator iter = bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (target.getDistance(p) == 1 && validTarget(p, false) &&
                // if it's not in line with us but is in line with the
                // target, then it's properly perpendicular
                (p.x != x && p.y != y) && (p.x == target.x || p.y == target.y)) {
                ShotEffect shot = shoot(bangobj, p);
                shot.newDamage /= 2; // our collateral damage is 50%
                shots.add(shot);
            }
        }
        return (shots.size() == 0) ? null :
            shots.toArray(new Effect[shots.size()]);
    }
}
