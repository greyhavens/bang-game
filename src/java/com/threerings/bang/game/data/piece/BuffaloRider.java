//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Point;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.ShotEffect;

import static com.threerings.bang.Log.log;

/**
 * Handles some special custom behavior needed for the Buffalo Rider.
 */
public class BuffaloRider extends Unit
{
    @Override // documentation inherited
    public float moveDamageScale (int dist)
    {
        // store this distance for later
        _moveDist = dist;
        // damage is scaled from 25% for no movement to 150% for full movement
        return (0.25f + 1.25f * dist / getMoveDistance());
    }

    @Override // documentation inherited
    public ShotEffect shoot (BangObject bangobj, Piece target, float scale)
    {
        int pushx = 2*target.x - x;
        int pushy = 2*target.y - y;
        // If we've moved at least 3 squares then try to push the target
        // If the target can't be pushed then add an extra .25 to the scle
        if (_moveDist >= 3 && !bangobj.board.isOccupiable(pushx, pushy)) {
            scale += 0.25f;
        }
        ShotEffect shot = super.shoot(bangobj, target, scale);
        if (_moveDist >= 3 && bangobj.board.isOccupiable(pushx, pushy)) {
            shot.pushx = pushx;
            shot.pushy = pushy;
        }
        return shot;
    }

    @Override // documentation inherited
    public boolean validTarget (Piece target, boolean allowSelf)
    {
        return !target.isAirborne() && super.validTarget(target, allowSelf);
    }

    protected int _moveDist;
}
