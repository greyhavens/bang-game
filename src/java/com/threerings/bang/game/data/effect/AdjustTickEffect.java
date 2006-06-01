//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.Iterator;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Adjusts the last acted tick for a particular piece.
 */
public class AdjustTickEffect extends Effect
{
    /** Identifies the use of this effect by the Staredown card. */
    public static final String STARED_DOWN = "cards/staredown/activate";

    /** Identifies the use of this effect by the Giddy Up card. */
    public static final String GIDDY_UPPED = "cards/giddy_up/activate";

    /** The coordinates at which we were activated. */
    public short x, y;

    /** The piece that we will be affecting. */
    public int pieceId;

    /** The new last acted time to assign to this piece. */
    public short newLastActed;

    public AdjustTickEffect ()
    {
    }

    public AdjustTickEffect (int x, int y, int delta)
    {
        this.x = (short)x;
        this.y = (short)y;
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
        for (Iterator iter = bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (p.x == x && p.y == y && p.isAlive() &&
                // make sure we're actually changing something
                p.lastActed != newLastActed) {
                pieceId = p.pieceId;
                break;
            }
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

    /** The delta from the board tick at activation time. */
    protected transient int _delta;
}
