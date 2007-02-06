//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.effect.Effect;


import static com.threerings.bang.Log.log;

/**
 * Clears a piece from the board.
 */
public class ExplodeEffect extends Effect
{
    /** The id of the piece to remove. */
    public int pieceId;
    
    public ExplodeEffect ()
    {
    }
    
    public ExplodeEffect (Piece piece)
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
        if (piece != null) {
            reportEffect(obs, piece, EXPLODE_EFFECT);
        }
        return true;
    }
    
    /** An effect reported on the primary target. */
    public static final String EXPLODE_EFFECT =
        "frontier_town/mushroom_cloud";
}
