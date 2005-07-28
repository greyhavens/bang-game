//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.client.sprite.UnitSprite;

/**
 * Handles the behavior of the cattle piece which is used in cattle
 * herding and other scenarios.
 */
public class Cattle extends Piece
{
    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new UnitSprite("cattle");
    }
}
