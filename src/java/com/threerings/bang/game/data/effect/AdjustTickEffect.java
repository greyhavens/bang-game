//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.Iterator;

import com.samskivert.util.IntIntMap;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Adjusts the last acted tick for a particular piece.
 */
public class AdjustTickEffect extends Effect
{
    /** Identifies the use of this effect by the Staredown card. */
    public static final String STARED_DOWN = "frontier_town/staredown";

    /** Identifies the use of this effect by the Giddy Up card. */
    public static final String GIDDY_UPPED = "frontier_town/giddy_up";

    /** The piece that we will be affecting. */
    public int pieceId;

    /** The new last acted time to assign to this piece. */
    public short newLastActed;

    public AdjustTickEffect ()
    {
    }

    public AdjustTickEffect (int pieceId, int delta)
    {
        this.pieceId = pieceId;
        _delta = delta;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        newLastActed = (short)(bangobj.tick + _delta);
        // make sure we're actually changing something
        Piece p = bangobj.pieces.get(pieceId);
        if (!p.isAlive() || p.lastActed == newLastActed) {
            pieceId = 0; // mark ourselves as inapplicable
        }
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return (pieceId > 0);
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer observer)
    {
        Piece piece = (Piece)bangobj.pieces.get(pieceId);
        if (piece != null) {
            String effect = (newLastActed < bangobj.tick) ?
                GIDDY_UPPED : STARED_DOWN;
            piece.lastActed = newLastActed;
            reportEffect(observer, piece, effect);
        }
        return true;
    }
    
    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null || piece.owner != pidx ||
            newLastActed < bangobj.tick) {
            return null;
        }
        return MessageBundle.compose("m.effect_staredown", piece.getName());
    }
    
    /** The delta from the board tick at activation time. */
    protected transient int _delta;
}
