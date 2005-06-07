//
// $Id$

package com.threerings.bang.data.piece;

/**
 * Handles some special custom behavior needed for the Windup Gunslinger.
 */
public class WindupSlinger extends Unit
{
    @Override // documentation inherited
    public boolean tick (short tick)
    {
        int odamage = damage;
        damage = Math.min(100, damage + 5);
        return (odamage != damage);
    }

    @Override // documentation inherited
    public boolean canActivateBonus ()
    {
        return false;
    }
}
