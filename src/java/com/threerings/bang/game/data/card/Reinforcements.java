//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.DuplicateGunslingerEffect;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * A card that allows the player to make a new Gunslinger that will not 
 * respawn and duplicates it's status from the selected unit.
 */
public class Reinforcements extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "reinforcements";
    }

    @Override // documentation inherited
    public boolean isValidPiece (BangObject bangobj, Piece target)
    {
        return (target instanceof Unit && target.isAlive());
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 30;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        DuplicateGunslingerEffect effect = new DuplicateGunslingerEffect();
        effect.pieceId = (Integer)target;
        return effect;
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
