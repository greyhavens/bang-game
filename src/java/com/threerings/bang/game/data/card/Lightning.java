//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.LightningEffect;
import com.threerings.bang.game.data.piece.Piece;

/**
 * A card that shoots lightning at a unit.
 */
public class Lightning extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "lightning";
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target.isTargetable() && target.isAlive());
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.INDIAN_POST;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 20;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        LightningEffect effect = new LightningEffect();
        effect.pieceId = (Integer)target;
        return effect;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 120;
    }

    @Override // documentation inherited
    public int getCoinCost ()
    {
        return 0;
    }
}
