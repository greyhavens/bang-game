//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.BuffaloRiderSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.MoveEffect;
import com.threerings.bang.game.data.effect.MoveShootEffect;
import com.threerings.bang.game.data.effect.ShotEffect;

/**
 * Handles some special custom behavior needed for the Buffalo Rider.
 */
public class BuffaloRider extends Unit
{
    @Override // documentation inherited
    public boolean shootsFirst ()
    {
        return true;
    }

    @Override // documentation inherited
    protected ShotEffect unitShoot (
            BangObject bangobj, Piece target, float scale)
    {
        return shoot(bangobj, target, scale, x, y);
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new BuffaloRiderSprite(_config.type);
    }

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

    @Override // documentation inherited
    public boolean killShot (BangObject bangobj, Piece target)
    {
        _killShot = true;
        int dist = getDistance(x, y, target.x, target.y);
        int damage = computeScaledDamage(
                bangobj, target, DISTANCE_DAMAGE_SCALE * dist);
        _killShot = false;
        return damage + target.damage >= 100;
    }

    /**
     * Generate a shot effect for the Buffalo Rider.
     */
    protected ShotEffect shoot (BangObject bangobj, Piece target, 
            float scale, short nx, short ny)
    {
        int dist = getDistance(x, y, target.x, target.y);
        scale *= DISTANCE_DAMAGE_SCALE * dist;
        short pushx = (short)(2*target.x - nx);
        short pushy = (short)(2*target.y - ny);
        String attackIcon = null;
        // If we've moved at least 3 squares then try to push the target
        // If the target can't be pushed then add an extra .25 to the scale
        boolean pushed = false;
        if (dist >= DISTANCE_TO_PUSH) {
            pushed = bangobj.board.canTravel(
                    target, target.x, target.y, pushx, pushy, true);
            if (!pushed && target.canBePushed()) {
                scale += DISTANCE_DAMAGE_SCALE;
                attackIcon = "smashed";
            }
        }
        ShotEffect shot = super.unitShoot(bangobj, target, scale);
        if (pushed && target.canBePushed()) {
            shot.pushx = pushx;
            shot.pushy = pushy;
        } else if (dist >= DISTANCE_TO_PUSH &&  !target.canBePushed()) {
            shot.appendIcon("unmovable", false);
        } else {
            shot.appendIcon(attackIcon, true);
        }
        return shot;
    }

    protected static final float DISTANCE_DAMAGE_SCALE = 0.25f;
    protected static final int DISTANCE_TO_PUSH = 4;
}
