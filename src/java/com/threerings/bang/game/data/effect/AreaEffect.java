//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Rectangle;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntIntMap;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * A base class for an effect that affects all pieces in a particular
 * area.
 */
public abstract class AreaEffect extends MultipleTargetEffect
{
    public int radius;
    public short x, y;

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
        for (Piece p : bangobj.pieces) {
            if (isPieceAffected(p) && p.getDistance(x, y) <= radius) {
                affected.add(p.pieceId);
            }
        }
        pieces = affected.toIntArray();
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        boolean success = true;
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece target = bangobj.pieces.get(pieces[ii]);
            if (target == null) {
                log.warning("Missing piece for area effect", "pid", pieces[ii], "effect", this);
                success = false;
                continue;
            }
            apply(bangobj, obs, ii, target, target.getDistance(x, y));
        }
        return success;
    }

    @Override // documentation inherited
    public Rectangle[] getBounds (BangObject bangobj)
    {
        return new Rectangle[] { new Rectangle(x - radius, y - radius,
                radius * 2 + 1, radius * 2 + 1) };
    }
}
