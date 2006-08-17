//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.MisfireEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A card that allows the player to cause a unit's next shot to injure themself.
 */
public class Misfire extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "misfire";
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && target.isAlive() && 
                ((Unit)target).getConfig().gunUser);
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.FRONTIER_TOWN;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 40;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 90;
    }

    @Override // documenataion inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        MisfireEffect effect = new MisfireEffect();
        effect.pieceId = (Integer)target;
        return effect;
    }
}
