//
// $Id$

package com.threerings.bang.game.data.piece;

import com.samskivert.util.ArrayUtil;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.client.sprite.CowSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.SpookEffect;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Handles the behavior of the cow piece which is used in cattle rustling and
 * other scenarios.
 */
public class Cow extends Piece
{
    /** Indicates whether or not this cow has been corralled. */
    public boolean corralled;

    /**
     * Called when a unit moves next to this cow; causes the cow to spook
     * in the opposite direction.
     */
    public SpookEffect spook (BangBoard board, Unit spooker)
    {
        // if we were spooked by a big shot, become owned by that player
        int owner = -1;
        if (spooker.getConfig().rank == UnitConfig.Rank.BIGSHOT) {
            owner = spooker.owner;
        }

        // otherwise spook in the opposite direction of our spooker
        for (int dd = 0; dd < DIRECTIONS.length; dd++) {
            if (spooker.x + DX[dd] == x && spooker.y + DY[dd] == y) {
                // spook in the direction that the spooker would have to move
                // to occupy our location (ie. if we're east of the spooker,
                // try spooking further east)
                return move(board, dd, owner, spooker.pieceId);
            }
        }

        return null;
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
    public Effect tick (short tick, BangBoard board, Piece[] pieces)
    {
        // if we're corralled, stop moving
        if (corralled) {
            return null;
        }

        // if we're walled in on all three sides, we want to move
        int walls = 0, direction = -1;
        for (int dd = 0; dd < DIRECTIONS.length; dd++) {
            if (board.isGroundOccupiable(x + DX[dd], y + DY[dd])) {
                // in the case that we're walled in on three sides, this will
                // only get assigned once, to the direction in which we are not
                // walled in
                direction = dd;
            } else {
                walls++;
            }
        }
        if (walls < 3) {
            direction = -1;
        }

        return (direction == -1) ? null : move(board, direction, -1, -1);
    }

    protected SpookEffect move (BangBoard board, int direction,
                                int owner, int spookerId)
    {
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
            if (whichDirection(hx, hy) == direction) {
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

        SpookEffect spook = new SpookEffect();
        spook.init(this);
        spook.owner = owner;
        spook.spookerId = spookerId;
        spook.nx = (short)nx;
        spook.ny = (short)ny;
        return spook;
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

    /** Used for temporary calculations. */
    protected static PointSet _moves = new PointSet();
}
