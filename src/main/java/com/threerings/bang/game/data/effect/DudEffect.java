//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.piece.Hindrance;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

/**
 * An effect that causes the next shot by a piece to have no effect.
 */
public class DudEffect extends SetHindranceEffect
{
    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return super.isApplicable() && _unit.getConfig().gunUser;
    }

    @Override // documentation inherited
    protected Hindrance createHindrance (Unit target)
    {
        return new Hindrance() {
            public String getName () {
                return "dud";
            }

            public ShotEffect shoot (BangObject bangobj, Unit shooter,
                    Piece target, float scale)
            {
                if (_expired) {
                    return null;
                }
                _expired = true;
                return new FailedShotEffect(shooter, target, 0, FailedShotEffect.DUD);
            }

            public boolean isExpired (short tick)
            {
                return _expired;
            }

            public boolean didAdjustAttack ()
            {
                return true;
            }

            boolean _expired = false;
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "frontier_town/dud";
    }

    @Override // from Effect
    public String getDescription (BangObject bangobj, int pidx)
    {
        // don't report to the player that the dud was played on their unit,
        // they'll find out when they next shoot
        return null;
    }
}
