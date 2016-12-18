//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.scenario.ScenarioInfo;

/**
 * The Joker: players put it on the board, where it looks like a card bonus.
 * When a unit tries to claim it, it explodes in a burst of the placing team's
 * color, causing damage and removing influences.
 */
public class Joker extends AddPieceCard
{
    @Override // documentation inherited
    public boolean shouldShowVisualization (int pidx)
    {
        return pidx == owner;
    }

    @Override // documentation inherited
    public String getType ()
    {
        return "joker";
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.FRONTIER_TOWN;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 35;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 30;
    }

    // documentation inherited
    protected Piece createPiece ()
    {
        return Bonus.createBonus(BonusConfig.getConfig("frontier_town/joker"), owner);
    }

    @Override // documentation inherited
    public boolean isPlayable (ScenarioInfo scenario, String townId)
    {
        return super.isPlayable(scenario, townId) && scenario.getTeams() != ScenarioInfo.Teams.COOP;
    }
}
