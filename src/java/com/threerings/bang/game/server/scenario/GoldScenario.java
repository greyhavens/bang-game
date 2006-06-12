//
// $Id$

package com.threerings.bang.game.server.scenario;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.ScenarioCodes;

import com.threerings.bang.game.data.effect.NuggetEffect;

import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.Counter;


/**
 * General functionality for scenarios using gold nuggets.
 */
public class GoldScenario extends Scenario
{
    @Override // documentation inherited
    protected void checkAdjustedCounter (BangObject bangobj, Unit unit)
    {
        if (_counters == null || _counters.size() == 0) {
            return;
        }

        // if this unit landed next to one of the counters, do some stuff
        Counter counter = null;
        for (Counter c : _counters) {
            if (c.getDistance(unit) <= 1) {
                counter = c;
                break;
            }
        }
        if (counter == null) {
            return;
        }

        // deposit or withdraw a nugget as appropriate
        NuggetEffect effect = null;
        if (counter.owner == unit.owner && unit.benuggeted) {
            effect = new NuggetEffect();
            effect.init(unit);
            effect.claimId = counter.pieceId;
            effect.dropping = true;
        } else if (allowClaimWithdrawal() && counter.owner != unit.owner &&
                   counter.count > 0 && unit.canActivateBonus(_nuggetBonus)) {
            effect = new NuggetEffect();
            effect.init(unit);
            effect.claimId = counter.pieceId;
            effect.dropping = false;
        }
        if (effect != null) {
            _bangmgr.deployEffect(unit.owner, effect);
        }
    }

    /**
     * If a scenario wishes to disable the withdrawal of nuggets from
     * opponents' claims it may override this method.
     */
    protected boolean allowClaimWithdrawal ()
    {
        return true;
    }

    @Override // documentation inherited
    protected int pointsPerCounter ()
    {
        return ScenarioCodes.POINTS_PER_NUGGET;
    }
}
