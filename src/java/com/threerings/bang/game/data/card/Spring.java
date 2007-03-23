//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Allows players to put a barely-visible spring on the board that
 * sends any unit that steps on it back to the location the unit came from.
 */
public class Spring extends AddPieceCard
{
    @Override // documentation inherited
    public boolean shouldShowVisualization (int pidx)
    {
        return pidx == owner;
    }

    @Override // documentation inherited
    public String getType ()
    {
        return "spring";
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.FRONTIER_TOWN;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 25;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 0;
    }

    // documentation inherited
    protected Piece createPiece ()
    {
        return Bonus.createBonus(BonusConfig.getConfig("frontier_town/spring"), owner);
    }
}
