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

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.UpdateEffect.class, "_piece", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.UpdateEffect.class, "_piece", this, out);
    }
}
