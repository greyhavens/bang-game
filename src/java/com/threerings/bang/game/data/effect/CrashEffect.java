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

    public CrashEffect (Piece piece, int damage, int crasher)
    {
        super(piece, damage);
        crasherId = crasher;
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
        } else {
            Piece crasher = bangobj.pieces.get(crasherId);
            if (crasher != null) {
                pidx = crasher.owner;
            }
            super.prepare(bangobj, dammap);
        }
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return pieceId != -1;
    }
}
