//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.util.MessageBundle;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Hindrance;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * An effect that sets a hindrance on the unit.
 */
public abstract class SetHindranceEffect extends BonusEffect
{
    @Override // from Effect
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    @Override // from Effect
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        super.prepare(bangobj, dammap);

        Piece piece = bangobj.pieces.get(pieceId);
        if (piece instanceof Unit) {
            _unit = (Unit)piece;
        }
    }

    @Override // from Effect
    public boolean isApplicable ()
    {
        return (_unit != null && _unit.hindrance == null);
    }

    @Override // from Effect
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        _unit = (Unit)bangobj.pieces.get(pieceId);
        if (_unit == null) {
            log.warning("Missing target for set hindrance effect " +
                        "[id=" + pieceId + "].");
            return false;
        }

        _unit.setHindrance(createHindrance(_unit), bangobj.tick);
        reportEffect(obs, _unit, getEffectName());
        return true;
    }

    @Override // from Effect
    public String getDescription (BangObject bangobj, int pidx)
    {
        if (_unit == null || _unit.owner != pidx || pidx == -1) {
            return null;
        }
        String name = _unit.hindrance.getName();
        return (name == null) ? null : MessageBundle.compose(
            "m.effect_influence", _unit.getName(), "m.hindrance_" + name);
    }

    @Override // from BonusEffect
    public int getBonusPoints ()
    {
        return 0; // maybe we should give negative points?
    }

    /** Creates the hindrance that will be applied to the target unit. */
    protected abstract Hindrance createHindrance (Unit target);

    /** Returns the name of the effect that will be reported. */
    protected abstract String getEffectName();

    protected transient Unit _unit;
}
