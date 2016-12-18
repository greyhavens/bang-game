//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.client.effect.IconInfluenceViz;
import com.threerings.bang.game.client.effect.InfluenceViz;
import com.threerings.bang.game.client.effect.ParticleInfluenceViz;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Hindrance;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that changes a steam unit's owner for one move (or four ticks,
 * whichever comes first).
 */
public class BuggyLogicEffect extends SetHindranceEffect
{
    /** The player taking control. */
    public int player;

    /** Restores the owner on expiration of the hindrance. */
    public static class ExpireEffect extends ExpireHindranceEffect
    {
        @Override // documentation inherited
        public boolean apply (BangObject bangobj, Observer obs)
        {
            Unit unit = (Unit)bangobj.pieces.get(pieceId);
            if (unit != null) {
                unit.setOwner(bangobj, unit.originalOwner);
            }
            return super.apply(bangobj, obs);
        }
    }

    public BuggyLogicEffect ()
    {
    }

    public BuggyLogicEffect (int player)
    {
        this.player = player;
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return super.isApplicable() && _unit.owner != player &&
            _unit.getConfig().make == UnitConfig.Make.STEAM;
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        _unit = (Unit)bangobj.pieces.get(pieceId);
        if (_unit != null) {
            _unit.setOwner(bangobj, player);
        }
        return super.apply(bangobj, obs);
    }

    @Override // documentation inherited
    protected Hindrance createHindrance (final Unit target)
    {
        return new Hindrance() {
            public String getName () {
                return "buggy_logic";
            }
            public InfluenceViz createViz (boolean high) {
                return (high ? 
                        new ParticleInfluenceViz("boom_town/buggy_logic") :
                        new IconInfluenceViz("buggy_logic"));
            }
            public Effect maybeGeneratePostOrderEffect () {
                ExpireInfluenceEffect effect = createExpireEffect();
                effect.pieceId = pieceId;
                return effect;
            }
            public ExpireInfluenceEffect createExpireEffect () {
                return new ExpireEffect();
            }
            public boolean isVisible () {
                return true;
            }
            protected int duration () {
                return RECOVERY_TICKS;
            }
        };
    }

    @Override // from Effect
    public String getDescription (BangObject bangobj, int pidx)
    {
        if (_unit == null || _unit.originalOwner != pidx || pidx == -1) {
            return null;
        }
        String name = _unit.getHindrance().getName();
        return (name == null) ? null : MessageBundle.compose(
            "m.effect_influence", _unit.getName(), "m.hindrance_" + name);
    }
    
    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "boom_town/buggy_logic";
    }

    /** The number of ticks it will take for the unit to recover if no order is
     * given. */
    protected static final int RECOVERY_TICKS = 4;
}
