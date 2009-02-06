//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.PeacePipeEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A card that prevents a unit from attacking until it is shot or the
 * hindrance expires. 
 */
public class PeacePipe extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "peace_pipe";
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
        return 20;
    }

    @Override // documentation inherited
    public Badge.Type getQualifier ()
    {
        return Badge.Type.TREES_SAVED_1;
    }

    @Override // documenataion inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        PeacePipeEffect effect = new PeacePipeEffect();
        effect.pieceId = (Integer)target;
        return effect;
    }
}
