//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ResurrectEffect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A card that allows the player to immediately resurrect one dead unit,
 * potentially stealing it from their opponent in the process.
 */
public class DustDevil extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "dust_devil";
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return 0;
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && !target.isAlive());
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.BOOM_TOWN;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 10;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 0;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        return new ResurrectEffect((Integer)target, owner);
    }
}
