//
// $Id$

package com.threerings.bang.game.data.piece;

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
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new MobileSprite("extras", "cow");
    }

    @Override // documentation inherited
    public boolean tick (short tick, BangBoard board, Piece[] pieces)
    {
        int nx = x, ny = y;

        // if there is a unit on any side of us, adjust our coordinates
        // accordingly
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (p instanceof Unit) {
                if (p.y == y) {
                    int cx = nx + adjust(p.x, x);
                    if (board.canOccupy(this, cx, ny)) {
                        nx = cx;
                    }
                }
                if (p.x == x) {
                    int cy = ny + adjust(p.y, y);
                    if (board.canOccupy(this, nx, cy)) {
                        ny = cy;
                    }
                }
            }
        }

        if (nx != x || ny != y) {
            board.updateShadow(this, null);
            position(nx, ny);
            board.updateShadow(null, this);
            return true;
        }
        return false;
    }

    /** Helper function for {@link #tick}. */
    protected int adjust (int a, int b)
    {
        int dv = a - b;
        return (dv == -1) ? 1 : ((dv == 1) ? -1 : 0);
    }
}
