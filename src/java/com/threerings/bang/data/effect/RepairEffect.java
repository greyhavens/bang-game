//
// $Id$

package com.threerings.bang.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.piece.Piece;

/**
 * An effect that repairs a particular piece on the board.
 */
public class RepairEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String REPAIRED = "repaired";

    public int pieceId;

    public void init (Piece piece)
    {
        pieceId = piece.pieceId;
    }

    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // nothing doing
    }

    public void apply (BangObject bangobj, Observer obs)
    {
        Piece piece = (Piece)bangobj.pieces.get(pieceId);
        if (piece == null) {
            return;
        }

        piece.damage = 0;
        reportEffect(obs, piece, REPAIRED);
    }
}
