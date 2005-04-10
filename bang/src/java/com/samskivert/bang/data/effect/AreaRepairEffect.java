//
// $Id$

package com.samskivert.bang.data.effect;

import com.samskivert.util.IntIntMap;

import com.samskivert.bang.data.BangObject;
import com.samskivert.bang.data.piece.Piece;

/**
 * An effect that does repair to all units within a certain area.
 */
public class AreaRepairEffect extends AreaEffect
{
    public int repair;

    public AreaRepairEffect ()
    {
    }

    public AreaRepairEffect (int repair, int radius, int x, int y)
    {
        super(radius, x, y);
        this.repair = repair;
    }

    @Override // documentation inherited
    protected void noteAffected (Piece piece, IntIntMap dammap)
    {
        // NOOP
    }

    @Override // documentation inherited
    protected void apply (
        BangObject bangobj, Observer obs, Piece piece, int dist)
    {
        int prepair = repair;
        for (int dd = 0; dd < dist; dd++) {
            prepair /= 2;
        }
        piece.damage = Math.max(0, piece.damage - repair);
        reportEffect(obs, piece, RepairEffect.REPAIRED);
    }
}
