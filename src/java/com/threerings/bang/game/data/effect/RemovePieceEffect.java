//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Removes a piece from the board.
 */
public class RemovePieceEffect extends Effect
{
    /** The piece id. */
    public int pieceId = -1;

    public RemovePieceEffect ()
    {
    }

    public RemovePieceEffect (Piece piece)
    {
        pieceId = piece.pieceId;
    }

    // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // nothing doing
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null) {
            log.warning("Unable to find piece to remove", "pieceId", pieceId);
            return false;
        }
        removeAndReport(bangobj, piece, obs);
        return true;
    }
}
