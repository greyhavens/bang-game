//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

/**
 * An effect that does repair to all units within a certain area.
 */
public class AreaRepairEffect extends AreaEffect
{
    /** The base amount by which to repair pieces. */
    public int baseRepair;

    /** The updated damage for the affected pieces. */
    public int[] newDamage;

    public AreaRepairEffect ()
    {
    }

    public AreaRepairEffect (int repair, int radius, int x, int y)
    {
        super(radius, x, y);
        baseRepair = repair;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        super.prepare(bangobj, dammap);

        // compute the new total damage for each affected piece
        newDamage = new int[pieces.length];
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece target = bangobj.pieces.get(pieces[ii]);
            int prepair = baseRepair / (target.getDistance(x, y)+1);
            newDamage[ii] = Math.max(0, target.damage - prepair);
        }
    }

    @Override // documentation inherited
    protected void apply (
        BangObject bangobj, Observer obs, int pidx, Piece piece, int dist)
    {
        piece.damage = newDamage[pidx];
        reportEffect(obs, piece, RepairEffect.REPAIRED);
    }

    @Override // documentation inherited
    protected boolean isPieceAffected (Piece piece)
    {
        return super.isPieceAffected(piece) && (piece.damage > 0);
    }
}
