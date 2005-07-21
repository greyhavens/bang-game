//
// $Id$

package com.threerings.bang.game.data.piece;

/**
 * Handles some special custom behavior needed for the Sharpshooter.
 */
public class Sharpshooter extends Unit
{
    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        // we do 150% of the damage of the gunslinger in exchange for not
        // being able to shoot immediately next door
        return 15 * super.computeDamage(target) / 10;
    }
}
