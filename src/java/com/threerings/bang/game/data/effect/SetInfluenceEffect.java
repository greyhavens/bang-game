//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that sets an influence on the unit that picks up a bonus.
 */
public abstract class SetInfluenceEffect extends BonusEffect
{
    /** The piece we will affect. */
    public int pieceId;

    @Override // documentation inherited
    public void init (Piece piece)
    {
        pieceId = piece.pieceId;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // nothing doing
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        Unit unit = (Unit)bangobj.pieces.get(pieceId);
        if (unit == null) {
            return;
        }

        unit.setInfluence(createInfluence(unit), bangobj.tick);
        reportEffect(obs, unit, getEffectName());
    }

    /** Creates the influence that will be applied to the target unit. */
    protected abstract Influence createInfluence (Unit target);

    /** Returns the name of the effect that will be reported. */
    protected abstract String getEffectName ();
}
