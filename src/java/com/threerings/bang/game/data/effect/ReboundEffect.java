//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Rectangle;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Sends the victim back to its last position.
 */
public class ReboundEffect extends BonusEffect
{
    /** The x and y coordinates to which the target was sent. */
    public short x, y;

    /** The x and y coordinates from which the target started. */
    public short ox, oy;
    
    @Override // documentation inherited
    public void init (Piece piece)
    {
        super.init(piece);
        ox = piece.x;
        oy = piece.y;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    @Override // documentation inherited
    public Rectangle getBounds ()
    {
        return new Rectangle(Math.min(ox, x), Math.min(oy, y),
                Math.abs(ox - x) + 1, Math.abs(oy - y) + 1);
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece target = bangobj.pieces.get(pieceId);
        if (target == null) {
            log.warning("Missing target for rebound effect " +
                "[id=" + pieceId + "].");
            return;
        }
        x = target.lastX;
        y = target.lastY;
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);
        Piece target = (Piece)bangobj.pieces.get(pieceId);
        if (target == null) {
            log.warning("Missing target for rebound effect " +
                "[id=" + pieceId + "].");
            return false;            
        }
        if (!bangobj.board.isOccupiable(x, y)) {
            log.warning("Attempting to rebound, but previous location is " +
                "unoccupiable! [target=" + target + ", x=" + x + ", y=" + y +
                "].");
            return false;
        }
        moveAndReport(bangobj, target, x, y, obs);
        return true;
    }
    
    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null || piece.owner != pidx) {
            return null;
        }
        return MessageBundle.compose("m.effect_spring", piece.getName());
    }
    
    @Override // documentation inherited
    protected String getActivatedEffect ()
    {
        return null;
    }
}
