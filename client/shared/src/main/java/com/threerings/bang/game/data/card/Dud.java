//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.DudEffect;
import com.threerings.bang.game.data.effect.Effect;

import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.scenario.ScenarioInfo;

/**
 * A card that allows the player to cause a unit's next shot to have no effect.
 */
public class Dud extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "dud";
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && target.isAlive() &&
                ((Unit)target).getConfig().gunUser);
    }

    @Override // documentation inherited
    public boolean shouldShowVisualization (int pidx)
    {
        return pidx == owner;
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.FRONTIER_TOWN;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 50;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 20;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        DudEffect effect = new DudEffect();
        effect.pieceId = (Integer)target;
        return effect;
    }

    @Override // documentation inherited
    public boolean isPlayable (ScenarioInfo scenario, String townId)
    {
        return super.isPlayable(scenario, townId) && scenario.getTeams() != ScenarioInfo.Teams.COOP;
    }
}
