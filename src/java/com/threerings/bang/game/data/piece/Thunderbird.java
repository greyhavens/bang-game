//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Point;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.ThunderbirdSprite;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.MoveEffect;
import com.threerings.bang.game.data.effect.MoveShootEffect;

import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Handles the special capabilities of the Thunderbird unit.
 */
public class Thunderbird extends Unit
{
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new ThunderbirdSprite(_config.type);
    }

    @Override // documentation inherited
    public boolean targetInRange (int nx, int ny, int tx, int ty)
    {
        return (getMoveDistance() >= 
            getDistance(tx, ty) + getDistance(nx, ny, tx, ty)) &&
            _attacks.contains(tx, ty);
    }

    @Override // documentation inherited
    public boolean checkLineOfSight (
            BangBoard board, int tx, int ty, Piece target)
    {
        return _attacks.contains(target.x, target.y);
    }

    @Override // documentation inherited
    public void computeMoves (
            BangBoard board, PointSet moves, PointSet attacks)
    {
        _attacks.clear();
        board.computeMoves(this, moves, _attacks);
        if (attacks != null) {
            attacks.addAll(_attacks);
        }
    }

    @Override // documentation inherited
    public Point computeShotLocation (BangBoard board, Piece target, 
            PointSet moveSet, boolean any, PointSet preferredSet)
    {
        // check if they are attackable
        if (!_attacks.contains(target.x, target.y)) {
            return null;
        }
        
        Point spot = null, prefer = null;
        int tdist = getDistance(target);
        int remain = getMoveDistance() - getDistance(target);
        int totdist = 0, tmove = Integer.MAX_VALUE;
        int ptotdist = 0, pmove = Integer.MAX_VALUE;
        // find the closest location (with least likelyhood for a u-turn)
        // where we can stop after attacking this target
        for (int ii = 0, ll = moveSet.size(); ii < ll; ii++) {
            int px = moveSet.getX(ii), py = moveSet.getY(ii);
            int dist = getDistance(px, py);
            int move = target.getDistance(px, py);
            if (move <= remain) {
                if (preferredSet.contains(px, py)) {
                    if (prefer == null) {
                        prefer = new Point(px, py);
                    } else if ((ptotdist < tdist) ?
                            (dist >= tdist || move < pmove) :
                            (dist <= ptotdist && move < pmove)) {
                        prefer.setLocation(px, py);
                    } else {
                        continue;
                    }
                    ptotdist = dist;
                    pmove = move;
                } else if (prefer == null) {
                    if (spot == null) {
                        spot = new Point(px, py);
                    } else if ((totdist < tdist) ? 
                            (dist >= tdist || move < tmove) :
                            (dist <= totdist && move < tmove)) {
                        spot.setLocation(px, py);
                    } else {
                        continue;
                    }
                    totdist = dist;
                    tmove = move;
                } else {
                    continue;
                }
                if (any) {
                    break;
                }
            }
        } 
        return (prefer != null) ? prefer : spot;
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
        effect.shotEffect = shoot(bangobj, target, 1f);
        effect.shotEffect.shooterLastActed = bangobj.tick;
        return effect;
    }

    /** Stores our valid attack locations */
    protected transient PointSet _attacks = new PointSet();
}
