//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.data.effect.SpringShotEffect;

/**
 * Handles some special custom behavior needed for the Forklift.
 */
public class Forklift extends Unit
{
    @Override // documentation inherited
    public boolean validTarget (
        BangObject bangobj, Piece target, boolean allowSelf)
    {
        return !target.isAirborne() &&
            super.validTarget(bangobj, target, allowSelf);
    }

    @Override // documentation inherited
    public boolean checkLineOfSight (
            BangBoard board, int tx, int ty, Piece target)
    {
        return board.canCross(tx, ty, target.x, target.y) &&
            super.checkLineOfSight(board, tx, ty, target);
    }
    
    @Override // documentation inherited
    public int computeScaledDamage (
            BangObject bangobj, Piece target, float scale)
    {
        // do a constant damage
        return computeDamage(target);
    }

    @Override // documentation inherited
    protected ShotEffect generateShotEffect (
            BangObject bangobj, Piece target, int damage)
    {
        if (target instanceof Unit) {
            return new SpringShotEffect(this, target, 0, null, null);
        } else {
            return new ShotEffect(
                this, target, damage, attackInfluenceIcons(), defendInfluenceIcons(target));
        }
    }
}
