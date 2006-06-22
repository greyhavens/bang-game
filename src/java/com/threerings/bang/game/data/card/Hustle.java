//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.HustleEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A card that allows the player to set a units action wait to 3 ticks. 
 */
public class Hustle extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "hustle";
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && target.isAlive());
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 25;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        HustleEffect effect = new HustleEffect();
        effect.pieceId = (Integer)target;
        return effect;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 400;
    }

    @Override // documentation inherited
    public int getCoinCost ()
    {
        return 0;
    }
}
