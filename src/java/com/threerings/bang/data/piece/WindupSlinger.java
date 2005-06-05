//
// $Id$

package com.threerings.bang.data.piece;

/**
 * Handles the state and behavior of the gun slinger piece.
 */
public class WindupSlinger extends Gunslinger
{
    @Override // documentation inherited
    public String getType ()
    {
        return "windupslinger";
    }

    @Override // documentation inherited
    public boolean tick (short tick)
    {
        int odamage = damage;
        damage = Math.min(100, damage + 5);
        return (odamage != damage);
    }

    @Override // documentation inherited
    public int getFireDistance ()
    {
        return 1;
    }

    @Override // documentation inherited
    public boolean canActivateBonus ()
    {
        return false;
    }
}
