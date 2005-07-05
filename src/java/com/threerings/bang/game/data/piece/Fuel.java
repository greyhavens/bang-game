//
// $Id$

package com.threerings.bang.game.data.piece;

/**
 * Handles the state and behavior of a basic fuel piece.
 */
public abstract class Fuel extends Piece
{
    /**
     * Returns the amount of energy the specified piece will get from
     * consuming this fuel and decrements that energy from the fuel.
     */
    public abstract int takeEnergy (Piece eater);

    @Override // documentation inherited
    public boolean preventsOverlap (Piece lapper)
    {
        return false;
    }
}
