//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AdjustTickEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A card that allows the player to move a unit again immediately.
 */
public class GiddyUp extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "giddy_up";
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 40;
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && target.isAlive());
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        return new AdjustTickEffect((Integer)target, -4);
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
