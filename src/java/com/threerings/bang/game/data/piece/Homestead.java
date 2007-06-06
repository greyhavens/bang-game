//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.client.sprite.HomesteadSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;

/**
 * Represents a Homestead piece in the Land Grab scenario.
 */
public class Homestead extends Prop
{
    /** Tracks our previous owner for scoring purposes. */
    public transient int previousOwner = -1;

    @Override // documentation inherited
    public void init ()
    {
        super.init();
        lastActed = Short.MAX_VALUE;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new HomesteadSprite();
    }

    @Override // documentation inherited
    public boolean isTargetable ()
    {
        return (owner != -1);
    }

    @Override // documentation inherited
    public boolean willBeTargetable ()
    {
        return true;
    }

    @Override // documentation inherited
    public int getTicksPerMove ()
    {
        return Integer.MAX_VALUE;
    }

    @Override // documentation inherited
    public boolean isOwnerConfigurable ()
    {
        return true;
    }

    @Override // documentation inherited
    public int getGoalRadius (BangObject bangobj, Piece mover)
    {
        return (owner == -1 && mover instanceof Unit &&
            ((Unit)mover).getConfig().rank == UnitConfig.Rank.BIGSHOT) ?
                +1 : -1;
    }

    @Override // documentation inherited
    public void wasKilled (short tick)
    {
        // clear out our ownership
        previousOwner = owner;
        owner = -1;

        // and reset our damage
        damage = 0;
    }

    @Override // documentation inherited
    public String getName ()
    {
        return MessageBundle.qualify(GameCodes.GAME_MSGS, "m.homestead");
    }
}
