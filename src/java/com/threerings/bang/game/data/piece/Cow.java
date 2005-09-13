//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Point;

import com.samskivert.util.ArrayUtil;
import com.threerings.util.RandomUtil;

import com.threerings.bang.game.client.sprite.CowSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Handles the behavior of the cow piece which is used in cattle herding
 * and other scenarios.
 */
public class Cow extends Piece
{
    /** Indicates whether or not this cow has been corralled. */
    public boolean corralled;

    /**
     * Called when a unit moves next to this cow; causes the cow to spook
     * in the opposite direction.
     */
    public void spook (Unit spooker)
    {
        // if we're already spooked, the first spooker retains precedence
        if (_wantToMove != -1) {
            return;
        }

        // otherwise spook in the opposite direction of our spooker
        for (int dd = 0; dd < DIRECTIONS.length; dd++) {
            if (spooker.x + DX[dd] == x && spooker.y + DY[dd] == y) {
                // spook in the direction that the spooker would have to
                // move to occupy our location (ie. if we're east of the
                // spooker, try spooking further east)
                _wantToMove = dd;
                log.info(info() + " spooked by " + spooker.info() +
                         " in " + _wantToMove);
                break;
            }
        }
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new CowSprite();
    }

    @Override // documentation inherited
    public int getMoveDistance ()
    {
        return 4;
    }

    @Override // documentation inherited
    public boolean tick (short tick, BangBoard board, Piece[] pieces)
    {
        // if we're corralled, stop moving
        if (corralled) {
            return false;
        }

        // if we're walled in on all three sides, we also move
        if (_wantToMove == -1) {
            int walls = 0;
            for (int dd = 0; dd < DIRECTIONS.length; dd++) {
                if (board.isGroundOccupiable(x + DX[dd], y + DY[dd])) {
                    // in the case that we're walled in on three sides,
                    // this will only get assigned once, to the direction
                    // in which we are not walled in
                    _wantToMove = dd;
                } else {
                    walls++;
                }
            }
            if (walls < 3) {
                _wantToMove = -1;
            }
        }

        // if we don't want to move, stop here
        if (_wantToMove == -1) {
            return false;
        }

        // otherwise look around for somewhere nicer to stand
        _moves.clear();
        board.computeMoves(this, _moves, null);
        int[] coords = _moves.toIntArray();
        ArrayUtil.shuffle(coords);

        // first look for a coordinate in the direction that we want to move
        int nx = x, ny = y;
        for (int ii = 0; ii < coords.length; ii++) {
            int hx = PointSet.decodeX(coords[ii]);
            int hy = PointSet.decodeY(coords[ii]);
            if (whichDirection(hx, hy) == _wantToMove) {
                nx = hx;
                ny = hy;
                break;
            }
        }

        // if that failed, go with anything that works
        if (nx == x && ny == y && coords.length > 0) {
            nx = PointSet.decodeX(coords[0]);
            ny = PointSet.decodeY(coords[0]);
        }

        if (nx != x || ny != y) {
            _wantToMove = -1;
            board.updateShadow(this, null);
            position(nx, ny);
            board.updateShadow(null, this);
            return true;
        }
        return false;
    }

    protected int whichDirection (int nx, int ny)
    {
        if (nx == x) {
            return (ny < y) ? NORTH : SOUTH;
        } else if (ny == y) {
            return (nx < x) ? WEST : EAST;
        }
        return -1;
    }

    /** Computed when a unit has moved near us and "spooked" us. */
    protected transient int _wantToMove = -1;

    /** Used for temporary calculations. */
    protected static PointSet _moves = new PointSet();
}
