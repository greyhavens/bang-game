//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Grants bonus points to the acquiring player.
 */
public class BonusPointEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String BONUS_POINT = "bonuses/bonus_point/activate";

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
        // grant points to the activating player
        bangobj.grantPoints(piece.owner, BONUS_POINTS);
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

    protected static final int BONUS_POINTS = 50;
}
