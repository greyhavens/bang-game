//
// $Id$

package com.threerings.bang.game.client.sprite;

import java.util.List;

import java.awt.Point;

import com.jme.math.Vector3f;

import com.threerings.bang.client.Config;

import com.threerings.bang.game.client.MoveShootHandler;

import com.threerings.bang.game.data.BangBoard;

import com.threerings.bang.game.data.piece.Piece;

import com.threerings.jme.sprite.LinePath;
import com.threerings.jme.sprite.Path;


/**
 * Sprite for the Thunderbird unit.
 */
public class ThunderbirdSprite extends UnitSprite
{
    public ThunderbirdSprite (String type)
    {
        super(type);
    }

    @Override // documentation inherited
    protected void startNextAction ()
    {
        super.startNextAction();
        // If we're dying, drop us to the ground while playing the dying
        // animation
        if (_action == "dying") {
            Vector3f air = new Vector3f(localTranslation),
                     ground = toWorldCoords(_piece.x, _piece.y,
                        _piece.computeElevation(_view.getBoard(),
                            _piece.x, _piece.y, false), new Vector3f());
            move(new LinePath(this, air, ground, _nextAction));
        }
    }

    @Override // documentation inherited
    protected Path createPath (BangBoard board)
    {
        if (_effectHandler != null) {
            // create a path that travels through the target
            Piece target = _tsprite.getPiece();
            List<Point> patha = null, pathb = null;
            if (board != null) {
                patha = board.computePath(_px, _py, target.x, target.y, _piece);
                if (patha != null) {
                    pathb = board.computePath(target.x, target.y, _piece);
                    if (pathb != null) {
                        pathb.remove(0);
                        _attackIdx = patha.size() - 1;
                        patha.addAll(pathb);
                        return createPath(
                                board, patha, Config.getMovementSpeed());
                    }
                }
            }
            // something's booched so fire off the shot now 
            ((MoveShootHandler)_effectHandler).fireShot();
            _effectHandler = null;
        }
        return super.createPath(board);
    }

    @Override // documentation inherited
    protected Path createPath (
            Vector3f[] coords, float[] durations, String action)
    {
        if (_effectHandler != null) {
            MoveShootHandler handler = (MoveShootHandler)_effectHandler;
            _effectHandler = null;
            return new ThunderbirdPath(this, coords, durations, _moveType, 
                    action, handler, _attackIdx);
        }
        return super.createPath(coords, durations, action);
    }

    @Override // documentation inherited
    protected void setCoord (BangBoard board, Vector3f[] coords, int idx,
                             int nx, int ny, boolean moving)
    {
        // Have it swoop down on the target
        if (idx == _attackIdx && _effectHandler != null) {
            Piece target = _tsprite.getPiece();
            coords[idx] = new Vector3f();
            toWorldCoords(nx, ny,
                    target.computeElevation(board, nx, ny, false) + 
                    (int)(0.5 * board.getElevationUnitsPerTile()),
                    coords[idx]);
        } else {
            super.setCoord(board, coords, idx, nx, ny, moving);
        }
    }

    /** The index on the path which is the target location. */
    protected int _attackIdx;
}
