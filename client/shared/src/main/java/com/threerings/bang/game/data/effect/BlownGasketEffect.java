//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.piece.Hindrance;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.client.effect.IconInfluenceViz;
import com.threerings.bang.game.client.effect.InfluenceViz;

/**
 * An effect that causes a steam unit to become immobile after six steps.
 */
public class BlownGasketEffect extends SetHindranceEffect
{
    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return super.isApplicable() &&
            _unit.getConfig().make == UnitConfig.Make.STEAM;
    }

    @Override // documentation inherited
    protected Hindrance createHindrance (final Unit target)
    {
        return new Hindrance() {
            public String getName () {
                return "blown_gasket";
            }
            public InfluenceViz createViz (boolean high) {
                return new IconInfluenceViz("blown_gasket");
            }
            public int adjustMoveDistance (int moveDistance) {
                return Math.min(moveDistance, _stepsRemaining);
            }
            public void didMove (int steps, short tick) {
                if ((_stepsRemaining -= steps) <= 0) {
                    _stepsRemaining = 0;
                    _startTick = tick;
                }
            }
            public boolean isVisible () {
                return true;
            }
            protected int duration () {
                return (_stepsRemaining > 0) ?
                    Short.MAX_VALUE : RECOVERY_TICKS;
            }
            protected int _stepsRemaining = STEP_LIMIT;
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "boom_town/blown_gasket";
    }

    /** The number of steps to which the hindrance limits the affected unit. */
    protected static final int STEP_LIMIT = 6;

    /** The number of ticks it takes for the unit to recover. */
    protected static final int RECOVERY_TICKS = 4;
}
