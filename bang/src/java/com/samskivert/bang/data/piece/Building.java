//
// $Id$

package com.samskivert.bang.data.piece;

import com.samskivert.bang.client.sprite.BuildingSprite;
import com.samskivert.bang.client.sprite.PieceSprite;

/**
 * A piece representing a building.
 */
public class Building extends BigPiece
{
    /**
     * Creates a building with the specified dimensions.
     */
    public Building (int width, int height)
    {
        super(width, height);
    }

    /** A constructor used when unserializing. */
    public Building ()
    {
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new BuildingSprite(getWidth(), getHeight());
    }
}
