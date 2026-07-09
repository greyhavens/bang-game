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

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.MultipleTargetEffect.class, "pieces", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AreaEffect.class, "radius", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AreaEffect.class, "x", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AreaEffect.class, "y", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AreaRepairEffect.class, "baseRepair", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.AreaRepairEffect.class, "newDamage", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.MultipleTargetEffect.class, "pieces", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AreaEffect.class, "radius", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AreaEffect.class, "x", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AreaEffect.class, "y", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AreaRepairEffect.class, "baseRepair", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.AreaRepairEffect.class, "newDamage", this, out);
    }
}
