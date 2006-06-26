//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Hindrance;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * An effect that sets a hindrance on the unit.
 */
public abstract class SetHindranceEffect extends Effect
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
        unit = (Unit)bangobj.pieces.get(pieceId);
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return (unit != null && unit.hindrance == null);
    } 

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        if (unit == null) {
            log.warning("Missing target for set hindrance effect " +
                        "[id=" + pieceId + "].");
            return false;
        }

        unit.setHindrance(createHindrance(unit), bangobj.tick);
        reportEffect(obs, unit, getEffectName());
        return true;
    }

    /** Creates the hindrance that will be applied to the target unit. */
    protected abstract Hindrance createHindrance (Unit target);

    /** Returns the name of the effect that will be reported. */
    protected abstract String getEffectName();

    protected transient Unit unit;
}
