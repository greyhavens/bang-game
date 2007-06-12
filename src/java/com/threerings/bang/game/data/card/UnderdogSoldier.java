//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.AddUnderdogSoldierEffect;
import com.threerings.bang.game.data.scenario.ScenarioInfo;

/**
 * A card that adds a computer controlled underdog soldier to the board.
 */
public class UnderdogSoldier extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "underdog_soldier";
    }

    @Override // documentation inherited
    public PlacementMode getPlacementMode ()
    {
        return PlacementMode.VS_AREA;
    }

    @Override // documentation inherited
    public boolean isValidLocation (BangObject bangobj, int tx, int ty)
    {
        return (bangobj.board.isOccupiable(tx, ty));
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.INDIAN_POST;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 15;
    }

    @Override // documentation inherited
    public boolean isPlayable (ScenarioInfo scenario, String townId)
    {
        return super.isPlayable(scenario, townId) && scenario.getTeams() != ScenarioInfo.Teams.COOP;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        int[] coords = (int[])target;
        return new AddUnderdogSoldierEffect(coords[0], coords[1]);
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 0;
    }
}
