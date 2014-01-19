//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Simply calls a piece to update itself
 */
public class UpdateEffect extends Effect
{
    public UpdateEffect()
    {
    }

    public UpdateEffect(Piece piece)
    {
        _piece = piece;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { _piece.pieceId };
    }

    public boolean apply (BangObject bangobj, Effect.Observer observer)
    {
        bangobj.pieces.updateDirect(_piece);
        reportEffect(observer, _piece, UPDATED);
        return true;
    }

    protected Piece _piece;
}
