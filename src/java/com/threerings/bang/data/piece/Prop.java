//
// $Id$

package com.threerings.bang.data.piece;

import com.threerings.bang.client.sprite.PropSprite;
import com.threerings.bang.client.sprite.PieceSprite;

/**
 * A piece representing a prop.
 */
public class Prop extends BigPiece
{
    /** The type of this prop (used to choose the right image). */
    public String type;

    /**
     * Creates a prop with the specified type and  dimensions.
     */
    public Prop (String type, int width, int height)
    {
        super(width, height);
        this.type = type;
    }

    /** A constructor used when unserializing. */
    public Prop ()
    {
    }

    @Override // documentation inherited
    public PieceSprite createSprite ()
    {
        return new PropSprite(type, getWidth(), getHeight());
    }

    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return !lapper.isFlyer();
    }
}
