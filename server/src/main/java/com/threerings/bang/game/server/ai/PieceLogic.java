//
// $Id$

package com.threerings.bang.game.server.ai;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import com.samskivert.util.RandomUtil;

import com.threerings.media.util.AStarPathUtil;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.game.server.BangManager;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Teleporter;
import com.threerings.bang.game.data.piece.Unit;

import com.threerings.bang.game.util.PointSet;

/**
 * Handles the logic for a single non-aligned AI piece.
 */
public abstract class PieceLogic
{
    /** Our piece ID. */
    public int pieceId;

    /**
     * Initializes the AI logic when the piece is created.
     */
    public void init (BangManager bangmgr, Piece piece)
    {
        _bangmgr = bangmgr;
        _bangobj = (BangObject)_bangmgr.getPlaceObject();
        pieceId = piece.pieceId;
    }

    /**
     * Called on every tick to let the AI move the piece.
     */
    public void tick (List<Piece> pieces, short tick)
    {
        for (Piece p : pieces) {
            if (p instanceof Unit && p.pieceId == pieceId && p.isAlive() &&
                    p.ticksUntilMovable(tick) == 0) {
                Unit unit = (Unit)p;
                _moves.clear();
                _attacks.clear();
                unit.computeMoves(_bangobj.board, _moves, _attacks);
                moveUnit(pieces, unit, _moves, _attacks);
            }
        }
    }

    /**
     * Moves an owned, ticked-up unit.
     *
     * @param pieces the array of pieces on the board
     * @param unit the unit to move
     * @param moves the places to which the unit can move
     * @param attacks the places the unit can attack
     */
    protected void moveUnit (List<Piece> pieces, Unit unit, PointSet moves, PointSet attacks)
    {
    }

    /**
     * Orders a unit to move.
     *
     * @param unit the unit to move
     * @param x the x coordinate to which to move or {@link Short#MAX_VALUE} if
     * the unit should be moved to the closest valid firing position to the
     * target.
     * @param y the y coordinate to which to move, this is ignored if {@link
     * Short#MAX_VALUE} is supplied for x.
     * @param target the piece for the unit to shoot, or <code>null</code> for
     * none
     * @return true if the order was executed, false if there was some error in
     * executing the order
     */
    protected boolean executeOrder (Unit unit, int x, int y, Piece target)
    {
        try {
            int targetId = (target == null) ? -1 : target.pieceId;
            _bangmgr.executeOrder(unit, x, y, targetId, true);
            return true;
        } catch (InvocationException e) {
            return false;
        }
    }

    /**
     * Gets the closest point to the provided destination that the unit can
     * reach in one move (or <code>null</code> if the destination is
     * unreachable).
     *
     * @param tdist the desired distance to the target: 0 to land on the
     * target, 1 to land next to the target, or, as a special case, -1 to
     * land within the unit's firing range
     */
    protected Point getClosestPoint (
        Unit unit, PointSet moves, int dx, int dy, int tdist)
    {
        return getClosestPoint(unit, moves, dx, dy, tdist, false);
    }

    /**
     * Gets the closest point to the provided destination that the unit can
     * reach in one move (or <code>null</code> if the destination is
     * unreachable).
     *
     * @param tdist the desired distance to the target: 0 to land on the
     * target, 1 to land next to the target, or, as a special case, -1 to
     * land within the unit's firing range
     * @param mustMove force the unit to move even if they're standing on
     * the closest point already
     */
    protected Point getClosestPoint (
        Unit unit, PointSet moves, int dx, int dy, int tdist, boolean mustMove)
    {
        int mindist = (tdist < 0) ? unit.getMinFireDistance() : 0,
            maxdist = (tdist < 0) ? unit.getMaxFireDistance() : tdist;

        // find out if we're already there
        int dist = Piece.getDistance(unit.x, unit.y, dx, dy);
        if (!mustMove && dist >= mindist && dist <= maxdist) {
            return new Point(unit.x, unit.y);
        }

        // next, look for points within the current move set that
        // satisfy the distance requirement
        ArrayList<Point> pts = new ArrayList<Point>();
        boolean close = false;
        for (int ii = 0, nn = moves.size(); ii < nn; ii++) {
            int mx = moves.getX(ii), my = moves.getY(ii);
            dist = Piece.getDistance(mx, my, dx, dy);
            if (dist >= mindist) {
                if (dist <= maxdist) {
                    pts.add(new Point(mx, my));
                }
            } else {
                close = true;
            }
        }
        if (pts.size() > 0) {
            return RandomUtil.pickRandom(pts);
        } else if (close) {
            // if there are points that are too close, pathfinding will not do
            // us any good
            return null;
        }

        // then do a search to find further points
        List<Point> path = AStarPathUtil.getPath(
                _bangobj.board, getStepper(), unit,
                getMaxLookahead(), unit.x, unit.y, dx, dy, true);
        if (path == null || path.size() < 2) {
            return null; // not even a partial path
        }
        Point pt = path.get(path.size() - 1);
        if (Piece.getDistance(pt.x, pt.y, dx, dy) > maxdist) {
            return null; // not close enough
        }
        for (int ii = path.size() - 1; ii >= 0; ii--) {
            pt = path.get(ii);
            if (moves.contains(pt.x, pt.y)) {
                return pt;
            }
        }
        return null;
    }

    /**
     * Returns the maximum lookahead for destinations beyond what units can
     * reach in the current tick.
     */
    protected int getMaxLookahead ()
    {
        return _bangobj.board.getWidth() / 2;
    }

    protected AStarPathUtil.Stepper getStepper ()
    {
        return new AStarPathUtil.Stepper() {
            public void considerSteps (int x, int y)
            {
                considerStep(x, y - 1, 1);
                considerStep(x - 1, y, 1);
                considerStep(x + 1, y, 1);
                considerStep(x, y + 1, 1);

                Teleporter teleporter = _bangobj.getTeleporters().get(
                        Piece.coord(x, y));
                if (teleporter == null) {
                    return;
                }
            }
        };
    }

    /** Reference to the Bang Manager. */
    protected BangManager _bangmgr;

    /** Reference to the game object. */
    protected BangObject _bangobj;

    /** Used to compute a unit's potential moves or attacks. */
    protected PointSet _moves = new PointSet(), _attacks = new PointSet();
}
