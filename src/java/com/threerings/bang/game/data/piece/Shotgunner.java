//
// $Id$

package com.threerings.bang.game.data.piece;

/**
 * Handles some special custom behavior needed for the Sharpshooter.
 */
public class Shotgunner extends Unit
{
    @Override // documentation inherited
    protected int computeDamage (Piece target)
    {
        // we do 150% in the immediately adjacent square, 50% damage two
        // squares out
        int dist = Math.abs(target.x - x) + Math.abs(target.y - y);
        int damage = super.computeDamage(target);
        return (dist == 1) ? (15 * damage / 10) : (damage / 2);
    }
}
