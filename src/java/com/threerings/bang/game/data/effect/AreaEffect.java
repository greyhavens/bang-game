//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntIntMap;
import com.threerings.media.util.MathUtil;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * A base class for an effect that affects all pieces in a particular
 * area.
 */
public abstract class AreaEffect extends Effect
{
    public int radius;
    public short x, y;
    public int[] pieces;

    public AreaEffect ()
    {
    }

    public AreaEffect (int radius, int x, int y)
    {
        this.radius = radius;
        this.x = (short)x;
        this.y = (short)y;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        ArrayIntSet affected = new ArrayIntSet();
        int r2 = radius * radius;
        for (Iterator iter = bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (affectedPiece(p) && MathUtil.distanceSq(p.x, p.y, x, y) <= r2) {
                affected.add(p.pieceId);
                noteAffected(p, dammap);
            }
        }
        pieces = affected.toIntArray();
    }

    /** Indicates whether or not we should affect this piece, assuming it
     * is in range. */
    protected boolean affectedPiece (Piece piece)
    {
        return (piece instanceof Unit && piece.owner >= 0 && piece.isAlive());
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece target = (Piece)bangobj.pieces.get(pieces[ii]);
            if (target == null) {
                log.warning("Missing piece for area effect [pid=" + pieces[ii] +
                            ", effect=" + this + "].");
                continue;
            }
            int dist = Math.abs(target.x - x) + Math.abs(target.y - y);
            apply(bangobj, obs, target, dist);
        }
    }

    /**
     * Called for every piece that is noted as being affected by {@link
     * #prepare}.
     */
    protected abstract void noteAffected (Piece piece, IntIntMap dammap);

    /**
     * Called for every piece to be affected by {@link #apply}.
     */
    protected abstract void apply (BangObject bangobj, Observer obs,
                                   Piece piece, int dist);
}
