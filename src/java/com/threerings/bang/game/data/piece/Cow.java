//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Point;

import com.samskivert.util.ArrayUtil;
import com.threerings.util.RandomUtil;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.util.PointSet;
import com.threerings.bang.game.data.BangBoard;

import static com.threerings.bang.Log.log;

/**
 * Handles the behavior of the cow piece which is used in cattle herding
 * and other scenarios.
 */
public class Cow extends Piece
{
    /** Indicates whether or not this cow has been corralled. */
    public boolean corralled;

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new MobileSprite("extras", "cow");
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

        _spot.setLocation(x, y);

        // if there is a unit on any side of us, we'll get spooked and run
        boolean wantToMove = false;
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (p instanceof Unit) {
                if ((p.x == x && (p.y == y-1 || p.y == y+1)) ||
                    (p.y == y && (p.x == x-1 || p.x == x+1))) {
                    wantToMove = true;
                    break;
                }
            }
        }

        // if we're walled in on all three sides, we also move
        int walls = 0;
        if (!board.isGroundOccupiable(x-1, y)) {
            walls++;
        }
        if (!board.isGroundOccupiable(x+1, y)) {
            walls++;
        }
        if (!board.isGroundOccupiable(x, y-1)) {
            walls++;
        }
        if (!board.isGroundOccupiable(x, y+1)) {
            walls++;
        }
        if (walls > 2) {
            wantToMove = true;
        }

        // if we don't want to move, stop here
        if (!wantToMove) {
            return false;
        }

        // otherwise look around for somewhere nicer to stand
        PointSet moves = new PointSet();
        board.computeMoves(this, moves, null);
        int[] coords = moves.toIntArray();
        ArrayUtil.shuffle(coords);
        for (int ii = 0; ii < coords.length; ii++) {
            int hx = PointSet.decodeX(coords[ii]);
            int hy = PointSet.decodeY(coords[ii]);
            // TODO: consider whether this is a desirable spot to stand
            _spot.x = hx;
            _spot.y = hy;
            break;
        }

        if (_spot.x != x || _spot.y != y) {
            board.updateShadow(this, null);
            position(_spot.x, _spot.y);
            board.updateShadow(null, this);
            return true;
        }
        return false;
    }

    /** Helper function for {@link #tick}. */
    protected boolean avoid (BangBoard board, Point spot, int x, int y)
    {
        boolean adjusted = false;
        if (spot.y == y) {
            int cx = spot.x + adjust(x, spot.x);
            if (board.canOccupy(this, cx, spot.y)) {
                spot.x = cx;
                adjusted = true;
            }
        }
        if (spot.x == x) {
            int cy = spot.y + adjust(y, spot.y);
            if (board.canOccupy(this, spot.x, cy)) {
                spot.y = cy;
                adjusted = true;
            }
        }
        return adjusted;
    }

    /** Helper function for {@link #avoid}. */
    protected int adjust (int a, int b)
    {
        int dv = a - b;
        return (dv == -1) ? 1 : ((dv == 1) ? -1 : 0);
    }

    /** Used for temporary calculations. */
    protected transient Point _spot = new Point();

    /** Used to track when this cow is "spooked" and will move around for
     * a few ticks. After each tick, it's spook level is decreased until
     * it finally stops moving again. */
    protected transient int _spookLevel;

    protected static final int[][] DORIENTS = {
        { 0, 1, -1 }, // foward, right, left
        { 0, -1, 1 }, // foward, left, right
        { 0, 1, -1 }, // foward, right, left
        { 0, -1, 1 }, // foward, left, right
        { 1, -1, 0 }, // right, left, forward
        { -1, 1, 0 }, // left, right, forward
    };

    /** The number of turns we'll move due to being spooked by a unit
     * landing next to us. */
    protected static final int UNIT_SPOOK_LEVEL = 3;
}
