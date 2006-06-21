//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.RepairEffect;
import com.threerings.bang.game.data.piece.Piece;

/**
 * A card that allows the player to repair a single unit.
 */
public class Repair extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "repair";
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return 0;
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target.isTargetable());
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 50;
    }

    @Override // documentation inherited
    public Effect activate (Object target)
    {
        return new RepairEffect((Integer)target);
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 150;
    }

    @Override // documentation inherited
    public int getCoinCost ()
    {
        return 0;
    }
}
