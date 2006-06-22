//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.IronPlateEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A card that allows the player to make a unit invincible for 7 ticks.
 */
public class IronPlate extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "iron_plate";
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && target.isAlive());
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 40;
    }

    @Override // documentation inherited
    public Effect activate (Object target)
    {
        IronPlateEffect effect = new IronPlateEffect();
        effect.pieceId = (Integer)target;
        return effect;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 250;
    }

    @Override // documentation inherited
    public int getCoinCost ()
    {
        return 0;
    }
}
