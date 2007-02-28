//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

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
        super(piece, damage);
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
    public boolean isApplicable ()
    {
        return pieceId != -1;
    }
}
