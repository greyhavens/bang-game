//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.Iterator;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Causes the piece in the area of our effect to delay one tick before
 * they are again able to move.
 */
public class StaredownEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String STARED_DOWN = "stared_down";

    /** The coordinates at which we were activated. */
    public short x, y;

    /** The piece that we will be affecting. */
    public int pieceId;

    /** The new last acted time to assign to this piece. */
    public short newLastActed;

    public StaredownEffect ()
    {
    }

    public StaredownEffect (int x, int y)
    {
        this.x = (short)x;
        this.y = (short)y;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        for (Iterator iter = bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (p.x == x && p.y == y && p.isAlive()) {
                pieceId = p.pieceId;
                newLastActed = (short)(p.lastActed+1);
                break;
            }
        }
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer observer)
    {
        Piece piece = (Piece)bangobj.pieces.get(pieceId);
        if (piece != null) {
            piece.lastActed = newLastActed;
            reportEffect(observer, piece, STARED_DOWN);
        }
    }
}
