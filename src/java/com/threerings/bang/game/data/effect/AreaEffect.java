//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Rectangle;

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
        for (Piece p : bangobj.pieces) {
            if (isPieceAffected(p) &&
                MathUtil.distanceSq(p.x, p.y, x, y) <= r2) {
                affected.add(p.pieceId);
            }
        }
        pieces = affected.toIntArray();
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

    @Override // documentation inherited
    public Rectangle getBounds (BangObject bangobj)
    {
        return new Rectangle(x - radius, y - radius, 
                radius * 2 + 1, radius * 2 + 1);
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        boolean success = true;
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece target = bangobj.pieces.get(pieces[ii]);
            if (target == null) {
                log.warning("Missing piece for area effect [pid=" + pieces[ii] +
                            ", effect=" + this + "].");
                success = false;
                continue;
            }
            apply(bangobj, obs, ii, target, target.getDistance(x, y));
        }
        return success;
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
