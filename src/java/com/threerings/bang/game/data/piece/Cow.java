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

        // if there is a unit on any side of us, adjust our coordinates
        // accordingly
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (p instanceof Unit) {
                avoid(board, _spot, p.x, p.y);
            }
        }

//         // if we're on the edge of the board, shy away from that as well
//         if (x == board.getWidth()-1) {
//             avoid(board, _spot, board.getWidth(), y);
//         } else if (x == 0) {
//             avoid(board, _spot, -1, y);
//         }
//         if (y == board.getHeight()-1) {
//             avoid(board, _spot, x, board.getHeight());
//         } else if (y == 0) {
//             avoid(board, _spot, x, -1);
//         }

        // if we haven't moved due to a unit scaring us off, randomly
        // continue moving some percentage of the time
        if (_spot.x == x && _spot.y == y && RandomUtil.getInt(100) < 50) {
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

        if (_spot.x != x || _spot.y != y) {
            board.updateShadow(this, null);
            position(_spot.x, _spot.y);
            board.updateShadow(null, this);
            return true;
        }
        return false;
    }

    /** Helper function for {@link #tick}. */
    protected void avoid (BangBoard board, Point spot, int x, int y)
    {
        if (spot.y == y) {
            int cx = spot.x + adjust(x, spot.x);
            if (board.canOccupy(this, cx, spot.y)) {
                spot.x = cx;
            }
        }
        if (spot.x == x) {
            int cy = spot.y + adjust(y, spot.y);
            if (board.canOccupy(this, spot.x, cy)) {
                spot.y = cy;
            }
        }
    }

    /** Helper function for {@link #avoid}. */
    protected int adjust (int a, int b)
    {
        int dv = a - b;
        return (dv == -1) ? 1 : ((dv == 1) ? -1 : 0);
    }

    protected transient Point _spot = new Point();

    protected static final int[][] DORIENTS = {
        { 0, 1, -1 }, // foward, right, left
        { 0, -1, 1 }, // foward, left, right
        { 0, 1, -1 }, // foward, right, left
        { 0, -1, 1 }, // foward, left, right
        { 1, -1, 0 }, // right, left, forward
        { -1, 1, 0 }, // left, right, forward
    };
}
