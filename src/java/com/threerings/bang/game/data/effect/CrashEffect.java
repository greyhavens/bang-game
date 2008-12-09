//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * When a dirigible comes crashing down.
 */
public class CrashEffect extends DamageEffect
{
    public int crasherId;

    public CrashEffect ()
    {
    }

    public CrashEffect (Piece piece, int damage, Piece crasher)
    {
        super(piece, Math.max(0, damage));
        crasherId = crasher.pieceId;
        pidx = crasher.owner;
    }

    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        return ArrayUtil.append(super.getWaitPieces(), crasherId);
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null || piece.damage >= 100) {
            pieceId = -1;
            return;
        }

        super.prepare(bangobj, dammap);
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        Piece piece = bangobj.pieces.get(crasherId);
        if (piece == null) {
            log.warning("Missing crasher for crash effect", "id", crasherId);
            return false;
        }
        removeAndReport(bangobj, piece, obs);
        return true;
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return pieceId != -1;
    }
}
