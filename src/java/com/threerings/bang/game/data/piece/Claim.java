//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.ClaimSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;

/**
 * A mining "claim" which is used in various scenarios.
 */
public class Claim extends Prop
{
    /** The number of nuggets left in this claim. */
    public int nuggets;

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new ClaimSprite(_type);
    }
}
