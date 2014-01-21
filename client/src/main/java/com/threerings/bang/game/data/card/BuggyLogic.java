//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.BuggyLogicEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Allows the player to give an order to another player's steam unit.
 */
public class BuggyLogic extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "buggy_logic";
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && target.isAlive() &&
                ((Unit)target).getConfig().make == UnitConfig.Make.STEAM &&
                target.owner != owner);
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.BOOM_TOWN;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 40;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 20;
    }

    @Override // documenataion inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        BuggyLogicEffect effect = new BuggyLogicEffect(owner);
        effect.pieceId = (Integer)target;
        return effect;
    }
}
