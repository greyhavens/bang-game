//
// $Id$

package com.samskivert.bang.data.piece;

import com.samskivert.bang.client.sprite.PieceSprite;
import com.samskivert.bang.client.sprite.SurpriseSprite;

/**
 * Represents an exciting surprise waiting to be picked up by a player on
 * the board. Surprises may generate full-blown effects or just influence
 * the piece that picked them up.
 */
public class Surprise extends Piece
{
    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return false;
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new SurpriseSprite("unknown");
    }
}
