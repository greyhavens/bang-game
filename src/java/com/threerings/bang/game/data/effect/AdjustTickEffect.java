//
// $Id$

package com.threerings.bang.game.data.effect;

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

    /** Identifies the use of this effect by the Half Giddy Up card. */
    public static final String HALF_GIDDY_UPPED =
        "frontier_town/half_giddy_up";
    
    /** The piece that we will be affecting. */
    public int pieceId;

    /** The delta from the board tick at activation time. */
    public int delta;
    
    /** The new last acted time to assign to this piece. */
    public short newLastActed;

    public AdjustTickEffect ()
    {
    }

    /**
     * Adjust a units lastActed tick.
     *
     * @param delta If delta < 0 then the lastActed will be set to lastActed + delta.  If
     * delta >= 0 then lastActed will be set to delta;
     */
    public AdjustTickEffect (int pieceId, int delta)
    {
        this.pieceId = pieceId;
        this.delta = delta;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece p = bangobj.pieces.get(pieceId);
        if (p == null || !p.isAlive()) {
            pieceId = 0;
            return;
        }
        if (delta >= 0) {
            newLastActed = (short)delta;
        } else {
            newLastActed = (short)Math.max(bangobj.tick - 4, p.lastActed + delta);
        }

        // make sure we're actually changing something
        if (p.lastActed == newLastActed) {
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
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece != null) {
            String effect;
            if (delta > 0) {
                effect = STARED_DOWN;
            } else {
                effect = (delta <= -4 ? GIDDY_UPPED : HALF_GIDDY_UPPED);
            }
            piece.lastActed = newLastActed;
            reportEffect(observer, piece, effect);
        }
        return true;
    }
    
    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null || piece.owner != pidx || pidx == -1 ||
            newLastActed < bangobj.tick) {
            return null;
        }
        return MessageBundle.compose("m.effect_staredown", piece.getName());
    }
}
