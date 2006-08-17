//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.IncreaseFireDistanceEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A card that allows a gun user to have increased fire distance.
 */
public class EagleEye extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "eagle_eye";
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.INDIAN_POST;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 30;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 0;
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && target.isAlive() &&
                ((Unit)target).getConfig().gunUser &&
                ((Unit)target).getConfig().maxFireDistance > 1);
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        IncreaseFireDistanceEffect effect = new IncreaseFireDistanceEffect();
        effect.pieceId = (Integer)target;
        return effect;
    }
}
