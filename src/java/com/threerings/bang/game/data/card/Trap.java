//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BonusConfig;
import com.threerings.bang.game.data.piece.Bonus;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Allows players to put a barely-visible trap on the board that
 * activates on contact.
 */
public class Trap extends AddPieceCard
{
    @Override // documentation inherited
    public boolean shouldShowVisualization (int pidx)
    {
        return pidx == owner;
    }

    @Override // documentation inherited
    public String getType ()
    {
        return "trap";
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.INDIAN_POST;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 60;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 20;
    }

    @Override // documentation inherited
    public Badge.Type getQualifier ()
    {
        return Badge.Type.TOTEMS_STACKED_1;
    }

    // documentation inherited
    protected Piece createPiece ()
    {
        return Bonus.createBonus(BonusConfig.getConfig("indian_post/trap"), owner);
    }
}
