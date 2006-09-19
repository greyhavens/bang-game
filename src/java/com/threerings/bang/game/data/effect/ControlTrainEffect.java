//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Track;
import com.threerings.bang.game.data.piece.Train;

import static com.threerings.bang.Log.log;

/**
 * Directs the train towards a given location.
 */
public class ControlTrainEffect extends Effect
{
    /** The index of the controlling player or -1 to clear the control. */
    public int player = -1;

    /** The desired train destination. */
    public transient int tx, ty;

    public ControlTrainEffect ()
    {
    }

    public ControlTrainEffect (int player, int tx, int ty)
    {
        this.player = player;
        this.tx = tx;
        this.ty = ty;
    }

    // documentation inherited
    public int[] getAffectedPieces ()
    {
        return NO_PIECES;
    }

    // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // no preparation required for releasing control
        if (player == -1) {
            return;
        }

        // find the train engine and destination track
        Train engine = null;
        Track dest = null;
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof Train &&
                ((Train)piece).type == Train.ENGINE) {
                engine = (Train)piece;
            } else if (piece instanceof Track && piece.intersects(tx, ty)) {
                dest = (Track)piece;
            }
        }
        if (engine == null) {
            log.warning("Missing train engine for control train effect " +
                "[player=" + player + "].");
            return;
        }

        // clear or compute the path
        if (player == -1) {
            engine.path = null;
            return;
        }
        if (dest == null) {
            log.warning("Missing destination track for control train effect " +
                "[player=" + player + ", tx=" + tx + ", ty=" + ty + "].");
            return;
        }
        if ((engine.path = engine.findPath(bangobj, dest)) == null) {
            log.warning("Couldn't find path for control train effect " +
                "[engine=" + engine + ", dest=" + dest + "].");
        }
    }

    // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        // update the owners for all train pieces
        for (Piece piece : bangobj.pieces) {
            if (piece instanceof Train) {
                piece.setOwner(bangobj, player);
                reportEffect(obs, piece, UPDATED);
            }
        }
        return true;
    }
}
