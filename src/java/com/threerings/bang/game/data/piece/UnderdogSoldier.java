//
// $Id$

package com.threerings.bang.game.data.piece;

/**
 * The underdog soldier.
 */
public class UnderdogSoldier extends DogSoldier
{
    @Override // documentation inherited
    public boolean canActivateBonus (Bonus bonus)
    {
        return false;
    }

    @Override // documentation inherited
    public String getLogic ()
    {
        return "com.threerings.bang.game.server.ai.UnderdogSoldierLogic";
    }
}
