//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.MarkerSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;

/**
 * A piece that marks a spawn location for bonuses.
 */
public class BonusMarker extends Piece
{
    public PieceSprite createSprite ()
    {
        return new MarkerSprite(false);
    }
}
