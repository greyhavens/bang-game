//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.NuggetEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A card that allows the player to make a unit drop a nugget 
 */
public class DropNugget extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "drop_nugget";
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && target.isAlive() &&
                NuggetEffect.NUGGET_BONUS.equals(((Unit)target).holding));
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 30;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        Unit unit = (Unit)bangobj.pieces.get((Integer)target);
        return NuggetEffect.dropNugget(bangobj, unit, -1);
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 300;
    }

    @Override // documentation inherited
    public int getCoinCost ()
    {
        return 0;
    }
}
