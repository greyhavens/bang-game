//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Clears a piece from the board.
 */
public class ClearPieceEffect extends Effect
{
    /** The id of the piece to remove. */
    public int pieceId;
    
    public ClearPieceEffect ()
    {
    }
    
    public ClearPieceEffect (Piece piece)
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
        // no-op
    }
    
    // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null) {
            log.warning("Missing piece for clear piece effect", "id", pieceId);
            return false;
        }
        removeAndReport(bangobj, piece, obs);
        return true;
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.ClearPieceEffect.class, "pieceId", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.ClearPieceEffect.class, "pieceId", this, out);
    }
}
