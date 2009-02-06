//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.BlownGasketEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A card that limits steam units' movement.
 */
public class BlownGasket extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "blown_gasket";
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && target.isAlive() && 
                ((Unit)target).getConfig().make == UnitConfig.Make.STEAM);
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.BOOM_TOWN;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 40;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 40;
    }

    @Override // documenataion inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        BlownGasketEffect effect = new BlownGasketEffect();
        effect.pieceId = (Integer)target;
        return effect;
    }
}
