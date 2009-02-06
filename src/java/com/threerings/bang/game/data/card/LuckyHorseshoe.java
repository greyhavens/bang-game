//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.LadyLuckEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A card that allows the player to give a unit a 50% chance at 2x damage
 * when attacking. 
 */
public class LuckyHorseshoe extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "lucky_horseshoe";
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && target.isAlive());
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
        return 40;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        LadyLuckEffect effect = new LadyLuckEffect();
        effect.pieceId = (Integer)target;
        return effect;
    }
}
