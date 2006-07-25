//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Point;

import com.threerings.bang.game.client.sprite.BuffaloRiderSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.MoveEffect;
import com.threerings.bang.game.data.effect.MoveShootEffect;
import com.threerings.bang.game.data.effect.ShotEffect;

import static com.threerings.bang.Log.log;

/**
 * Handles some special custom behavior needed for the Buffalo Rider.
 */
public class BuffaloRider extends Unit
{
    @Override // documentation inherited
    public ShotEffect shoot (BangObject bangobj, Piece target, float scale)
    {
        return shoot(bangobj, target, scale, x, y);
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new BuffaloRiderSprite(_config.type);
    }

    @Override // documentation inherited
    public boolean validTarget (Piece target, boolean allowSelf)
    {
        return !target.isAirborne() && super.validTarget(target, allowSelf);
    }

    @Override // documentation inherited
    public boolean checkLineOfSight (
            BangBoard board, int tx, int ty, Piece target)
    {
        return board.canCross(tx, ty, target.x, target.y) &&
            super.checkLineOfSight(board, tx, ty, target);
    }

    @Override // documentation inherited
    public MoveEffect generateMoveEffect (
            BangObject bangobj, int nx, int ny, Piece target)
    {
        if (target == null) {
            return super.generateMoveEffect(bangobj, nx, ny, target);
        }
        MoveShootEffect effect = new MoveShootEffect();
        effect.init(this);
        effect.nx = (short)nx;
        effect.ny = (short)ny;
        effect.shotEffect = shoot(bangobj, target, 1f, effect.nx, effect.ny);
        effect.shotEffect.shooterLastActed = bangobj.tick;
        return effect;
    }

    /**
     * Generate a shot effect for the Buffalo Rider.
     */
    protected ShotEffect shoot (BangObject bangobj, Piece target, 
            float scale, short nx, short ny)
    {
        int dist = getDistance(x, y, nx, ny);
        scale *= (0.25f + 0.25f * dist);
        short pushx = (short)(2*target.x - nx);
        short pushy = (short)(2*target.y - ny);
        // If we've moved at least 3 squares then try to push the target
        // If the target can't be pushed then add an extra .25 to the scle
        boolean pushed = false;
        if (dist >= 3 && bangobj.board.canTravel(
                    target, target.x, target.y, pushx, pushy, true)) {
            pushed = true;
            scale += 0.25f;
        }
        ShotEffect shot = super.shoot(bangobj, target, scale);
        if (pushed) {
            shot.pushx = pushx;
            shot.pushy = pushy;
        }
        return shot;
    }
}
