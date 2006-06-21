//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AdjustTickEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.Piece;

/**
 * A card that allows the player to delay by one tick the action of any
 * piece on the board.
 */
public class Staredown extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "staredown";
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return 0;
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && target.isAlive());
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 50;
    }

    @Override // documentation inherited
    public Effect activate (Object target)
    {
        return new AdjustTickEffect((Integer)target, 0);
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 100;
    }

    @Override // documentation inherited
    public int getCoinCost ()
    {
        return 0;
    }
}
