//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.HomesteadSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangObject;

/**
 * Represents a Homestead piece in the Land Grab scenario.
 */
public class Homestead extends Prop
{
    /** Tracks our previous owner for scoring purposes. */
    public transient int previousOwner = -1;

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
    public void wasKilled (BangObject bangobj, int shooterId)
    {
        // clear out our ownership
        previousOwner = owner;
        owner = -1;

        // and reset our damage
        damage = 0;
    }
}
