//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.NightmareShotEffect;
import com.threerings.bang.game.data.effect.RepairEffect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.util.PointSet;

/**
 * Handles the special abilities of the Dream Catcher.
 */
public class DreamCatcher extends BallisticUnit
{
    @Override // documentation inherited
    public boolean validTarget (
            BangObject bangobj, Piece target, boolean allowSelf)
    {
        return (target instanceof Unit) &&
            super.validTarget(bangobj, target, allowSelf);
    }

    @Override // documentation inherited
    public int computeScaledDamage (
            BangObject bangobj, Piece target, float scale)
    {
        // damage level, influences, and hindrances do not affect the
        // Dream Catcher's absorption attack
        return computeDamage(target);
    }

    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        // likewise with attack and defend modifiers
        return _config.damage;
    }

    @Override // documentation inherited
    protected ShotEffect unitShoot (
            BangObject bangobj, Piece target, float scale)
    {
        // any damage she does is absorbed as health
        ShotEffect shot = super.unitShoot(bangobj, target, 1f);
        int tdamage = shot.newDamage - target.damage;
        if (tdamage > 0 && damage > 0) {
            RepairEffect reffect = new RepairEffect(pieceId);
            reffect.baseRepair = tdamage;
            shot.preShotEffects = new Effect[] { reffect };
        }

        // She will reset the target's tick counter and force them to move.
        // They will try to move away from her, where their movement distance
        // is inversely proportional to the amount their tick was reset
        int tickDelta = Math.max(0, Math.min(
                    bangobj.tick - target.lastActed,
                    target.getTicksPerMove()));
        double tickRatio = 1.0 - (double)tickDelta /
            (double)target.getTicksPerMove();
        int move = (int)Math.ceil(tickRatio * target.getMoveDistance());
        if (target.canBePushed()) {
            PointSet moves = new PointSet();
            bangobj.board.computeMoves(target, moves, null, move);
            int dist = 0;
            int nx = -1, ny = -1;
            for (int ii = 0; ii < moves.size(); ii++) {
                int x = moves.getX(ii);
                int y = moves.getY(ii);
                int d = getDistance(x, y);
                if (d > dist) {
                    nx = x;
                    ny = y;
                    dist = d;
                }
            }

            if (nx != target.x || ny != target.y) {
                shot.pushx = (short)nx;
                shot.pushy = (short)ny;
                shot.pushAnim = false;
            }

            if (target.lastActed < bangobj.tick) {
                shot.targetLastActed = bangobj.tick;
            }
        } else if (move > 0) {
            shot.appendIcon("unmovable", false);
        }

        return shot;
    }

    @Override // documentation inherited
    protected ShotEffect generateShotEffect (
            BangObject bangobj, Piece target, int damage)
    {
        return new NightmareShotEffect(this, target, damage,
                attackInfluenceIcons(), defendInfluenceIcons(target));
    }
}
