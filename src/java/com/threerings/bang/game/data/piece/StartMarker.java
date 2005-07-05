//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.client.sprite.MarkerSprite;
import com.threerings.bang.game.client.sprite.PieceSprite;

/**
 * A piece that marks a starting location for one of the players.
 */
public class StartMarker extends Piece
{
    public PieceSprite createSprite ()
    {
        return new MarkerSprite(true);
    }
}
