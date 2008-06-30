//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

/**
 * A base class for an effect that affects all pieces in a particular
 * area.
 */
public abstract class MultipleTargetEffect extends Effect
{
    public int[] pieces;

    public MultipleTargetEffect ()
    {
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return (pieces.length > 0);
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return pieces;
    }

    /**
     * Indicates whether or not we should affect this piece, assuming it is in
     * range.
     */
    protected boolean isPieceAffected (Piece piece)
    {
        return piece.isAlive() && piece.isTargetable();
    }

    /**
     * Called for every piece to be affected by {@link #apply}.
     */
    protected abstract void apply (BangObject bangobj, Observer obs,
                                   int pidx, Piece piece, int dist);
}
