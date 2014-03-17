//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.BangObject;

/**
 * The underdog soldier.
 */
public class UnderdogSoldier extends DogSoldier
{
    @Override // documentation inherited
    public boolean canActivateBonus (BangObject bangobj, Bonus bonus)
    {
        return false;
    }

    @Override // documentation inherited
    public String getLogic ()
    {
        return "com.threerings.bang.game.server.ai.UnderdogSoldierLogic";
    }
}
