//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Point;

import com.threerings.util.RandomUtil;

import com.threerings.bang.game.client.sprite.MobileSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
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
    public boolean tick (short tick, BangBoard board, Piece[] pieces)
    {
        // if we're corralled, stop moving
        if (corralled) {
            return false;
        }

        _spot.setLocation(x, y);

        // if there is a unit on any side of us, we'll get spooked and run
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (p instanceof Unit) {
                if (avoid(board, _spot, p.x, p.y)) {
                    _spookLevel = UNIT_SPOOK_LEVEL;
                }
            }
        }

        // if we're on the edge of the board, shy away from that as well
        if (x == board.getWidth()-1) {
            avoid(board, _spot, board.getWidth(), y);
        } else if (x == 0) {
            avoid(board, _spot, -1, y);
        }
        if (y == board.getHeight()-1) {
            avoid(board, _spot, x, board.getHeight());
        } else if (y == 0) {
            avoid(board, _spot, x, -1);
        }

        // if we haven't moved due to a unit scaring us off, keep moving
        // as long as we're spooked
        if (_spot.x == x && _spot.y == y && _spookLevel > 0) {
            // pick a set of candidate movements
            int[] dorient = DORIENTS[RandomUtil.getInt(DORIENTS.length)];
            // go down the list looking for one that works
            for (int ii = 0; ii < dorient.length; ii++) {
                int norient = (orientation + dorient[ii] + 4) % 4;
                int nx = x + FWD_X_MAP[norient];
                int ny = y + FWD_Y_MAP[norient];
                if (board.canOccupy(this, nx, ny)) {
                    _spot.x = nx;
                    _spot.y = ny;
                    break;
                }
            }
        }

        // decrement our spook level if we've been previously spooked
        if (_spookLevel > 0) {
            _spookLevel--;
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
