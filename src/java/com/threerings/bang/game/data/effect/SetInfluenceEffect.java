//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * An effect that sets an influence on the unit that picks up a bonus.
 */
public abstract class SetInfluenceEffect extends BonusEffect
{
    /** The type of influence to affect the unit. */
    public Unit.InfluenceType influenceType = Unit.InfluenceType.MAIN;
    
    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Unit unit = (Unit)bangobj.pieces.get(pieceId);
        // only grant points if the unit doesn't already have an influence
        if (unit != null && unit.getInfluences()[influenceType.ordinal()] == null) {
            super.prepare(bangobj, dammap);
        }
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        Unit unit = (Unit)bangobj.pieces.get(pieceId);
        if (unit == null) {
            log.warning("Missing target for set influence effect", "id", pieceId);
            return false;
        }

        Influence influence = createInfluence(unit);
        influence.init(bangobj.tick);
        unit.setInfluence(influenceType, influence);
        reportEffect(obs, unit, getEffectName());
        return true;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (!(piece instanceof Unit) || piece.owner != pidx || pidx == -1) {
            return null;
        }
        String name = null;
        Unit unit = ((Unit)piece);
        if (unit.getInfluences()[influenceType.ordinal()] != null)
        {
            name = unit.getInfluences()[influenceType.ordinal()].getName();
        }
        return (name == null) ? null : MessageBundle.compose(
            "m.effect_influence", piece.getName(), "m.influence_" + name);
    }

    /** Creates the influence that will be applied to the target unit. */
    protected abstract Influence createInfluence (Unit target);

    /** Returns the name of the effect that will be reported. */
    protected abstract String getEffectName ();
}
