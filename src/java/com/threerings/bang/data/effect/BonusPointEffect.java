//
// $Id$

package com.threerings.bang.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.piece.Piece;

/**
 * Grants a bonus point to the acquiring player.
 */
public class BonusPointEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String BONUS_POINT = "bonus_point";

    public int pieceId;

    @Override // documentation inherited
    public void init (Piece piece)
    {
        pieceId = piece.pieceId;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece piece = (Piece)bangobj.pieces.get(pieceId);
        if (piece == null) {
            return;
        }
        // grant a bonus point to the activating player
        bangobj.setPointsAt(bangobj.points[piece.owner] + 1, piece.owner);
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        Piece piece = (Piece)bangobj.pieces.get(pieceId);
        if (piece == null) {
            return;
        }
        reportEffect(obs, piece, BONUS_POINT);
    }
}
