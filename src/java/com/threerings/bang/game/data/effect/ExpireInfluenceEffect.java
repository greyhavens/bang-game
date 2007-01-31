//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.BangObject;

/**
 * Expires the currently active influence on a unit.
 */
public class ExpireInfluenceEffect extends Effect
{
    /** The identifier of the piece to be repaired. */
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
        // nothing doing
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        Unit unit = (Unit)bangobj.pieces.get(pieceId);
        if (unit != null) {
            unit.setMainInfluence(null);
            reportEffect(obs, unit, UPDATED);
        }
        return true;
    }
}
