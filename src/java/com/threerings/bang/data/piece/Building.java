//
// $Id$

package com.threerings.bang.data.piece;

import com.threerings.bang.client.sprite.BuildingSprite;
import com.threerings.bang.client.sprite.PieceSprite;

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

    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return !(lapper instanceof Dirigible);
    }
}
