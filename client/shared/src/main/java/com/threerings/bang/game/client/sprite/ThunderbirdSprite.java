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
    public boolean updatePosition (BangBoard board)
    {
        if (_effectHandler == null) {
            return super.updatePosition(board);
        } else if (_fastAnimation) {
            ((MoveShootHandler)_effectHandler).fireShot();
            _effectHandler = null;
            return super.updatePosition(board);
        }

        // we might move but end up in the same spot
        moveSprite(board);
        return isMoving();
    }

    @Override // documentation inherited
    protected void moveSprite (BangBoard board)
    {
        if (_effectHandler == null || _fastAnimation) {
            super.moveSprite(board);
            return;
        }

        // We want to be able to generate return paths
        if (!isMoving()) {
            Path path = createPath(board);
            if (path != null) {
                move(path);
                _px = _piece.x;
                _py = _piece.y;

            } else {
                setLocation(board, _piece.x, _piece.y);
            }
        }
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
                if (patha != null && patha.size() > 1) {
                    pathb = board.computePath(target.x, target.y, _piece);
                    if (pathb != null && pathb.size() > 1) {
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
