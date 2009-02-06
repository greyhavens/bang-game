//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.AdjustMoveInfluenceEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A card that allows the player to give a unit a +3 movement bonus for their
 * next move.
 */
public class JackRabbit extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "jack_rabbit";
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && target.isAlive());
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.INDIAN_POST;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 50;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 30;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        AdjustMoveInfluenceEffect effect =
            new AdjustMoveInfluenceEffect(3, 4);
        effect.pieceId = (Integer)target;
        effect.icon = "jack_rabbit";
        effect.name = "indian_post/jack_rabbit";
        return effect;
    }
}
