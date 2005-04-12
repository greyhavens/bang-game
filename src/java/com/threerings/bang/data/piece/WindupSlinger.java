//
// $Id$

package com.threerings.bang.data.piece;

import com.threerings.bang.client.sprite.PieceSprite;
import com.threerings.bang.client.sprite.UnitSprite;

/**
 * Handles the state and behavior of the gun slinger piece.
 */
public class WindupSlinger extends Gunslinger
{
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new UnitSprite("windupslinger");
    }

    @Override // documentation inherited
    public int getFireDistance ()
    {
        return 1;
    }
}
