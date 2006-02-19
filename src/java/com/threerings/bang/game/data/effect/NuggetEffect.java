//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Causes the activating piece to "pick up" the nugget.
 */
public class NuggetEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String BENUGGETED = "bonuses/nugget/activate";

    public int pieceId;

    @Override // documentation inherited
    public void init (Piece piece)
    {
        pieceId = piece.pieceId;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece piece = (Piece)bangobj.pieces.get(pieceId);
        if (piece instanceof Unit) {
            // mark the target piece as benuggeted now as they may have
            // landed on a nugget which was right next to a claim and the
            // claim needs to know not to give them another nugget before
            // this effect will be applied; we'll need to benugget them
            // again in apply to ensure that it happens on the client
            ((Unit)piece).benuggeted = true;
        }
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        Piece piece = (Piece)bangobj.pieces.get(pieceId);
        if (!(piece instanceof Unit)) {
            return;
        }

        ((Unit)piece).benuggeted = true;
        reportEffect(obs, piece, BENUGGETED);
    }
}
