//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.PowerUpEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A card that allows the player to give a unit 30% increase to attack damage.
 */
public class HollowPoint extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "hollow_point";
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && target.isAlive());
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.FRONTIER_TOWN;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 60;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 25;
    }

    @Override // documentation inherited
    public Badge.Type getQualifier ()
    {
        return Badge.Type.UNITS_KILLED_1;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        PowerUpEffect effect = new PowerUpEffect();
        effect.pieceId = (Integer)target;
        return effect;
    }
}
