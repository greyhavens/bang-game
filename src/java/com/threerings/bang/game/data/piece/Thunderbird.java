//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Point;
import java.awt.Rectangle;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.ThunderbirdSprite;

import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.MoveEffect;
import com.threerings.bang.game.data.effect.MoveShootEffect;

import com.threerings.bang.game.util.PointSet;

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
        return (getMoveDistance() >= getDistance(tx, ty) + getDistance(nx, ny, tx, ty)) &&
            _attacks.contains(tx, ty);
    }

    @Override // documentation inherited
    public boolean shootsFirst ()
    {
        return true;
    }

    @Override // documentation inherited
    public boolean checkLineOfSight (BangBoard board, int tx, int ty, Piece target)
    {
        return _attacks.contains(target.x, target.y);
    }

    @Override // documentation inherited
    public void computeMoves (BangBoard board, PointSet moves, PointSet attacks)
    {
        _attacks.clear();
        board.computeMoves(this, moves, _attacks);
        if (attacks != null) {
            attacks.addAll(_attacks);
        }
    }

    @Override // documentation inherited
    public Point computeShotLocation (BangBoard board, Piece target, PointSet moveSet, boolean any,
                                      PointSet preferredSet)
    {
        // check that they are attackable
        if (target == null || !_attacks.contains(target.x, target.y)) {
            return null;
        }
        
        Point spot = null, prefer = null;
        int tdist = getDistance(target);
        int remain = getMoveDistance() - getDistance(target);
        int tmove = Integer.MAX_VALUE, pmove = Integer.MAX_VALUE;

        // first check if we can fire while returning to the same spot
        if (moveSet.contains(x, y)) {
            if (tdist <= remain) {
                spot = new Point(x, y);
                if (preferredSet.isEmpty() || preferredSet.contains(x, y) || any) {
                    return spot;
                }
                tmove = 0;
            }
        }
        
        // next seach the move set for the closest location
        for (int ii = 0, ll = moveSet.size(); ii < ll; ii++) {
            int px = moveSet.getX(ii), py = moveSet.getY(ii);
            int move = target.getDistance(px, py);
            if (move <= remain) {
                if (move < pmove && preferredSet.contains(px, py)) {
                    if (prefer == null) {
                        prefer = new Point();
                    }
                    prefer.setLocation(px, py);
                    pmove = move;
                } else if (move < tmove && prefer == null) {
                    if (spot == null) {
                        spot = new Point();
                    }
                    spot.setLocation(px, py);
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
    public PointSet computeShotRange (BangBoard board, int dx, int dy)
    {
        PointSet ps = new PointSet();
        int move = getMoveDistance();
        int x1 = dx - move, x2 = dx + move, y1 = dy - move, y2 = dy + move;
        Rectangle playarea = board.getPlayableArea();
        for (int xx = x1; xx <= x2; xx++) {
            for (int yy = y1; yy <= y2; yy++) {
                if (!playarea.contains(xx, yy) ||
                    getDistance(xx, yy) + getDistance(xx, yy, dx, dy) > move) {
                    continue;
                }
                ps.add(xx, yy);
            }
        }
        return ps;
    }

    @Override // documentation inherited
    public MoveEffect generateMoveEffect (BangObject bangobj, int nx, int ny, Piece target)
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
