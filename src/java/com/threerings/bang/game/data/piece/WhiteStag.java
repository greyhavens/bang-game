//
// $Id$

package com.threerings.bang.game.data.piece;

/**
 * The white stag.
 */
public class WhiteStag extends BuffaloRider
{
    @Override // documentation inherited
    public boolean canActivateBonus (Bonus bonus)
    {
        return false;
    }

    @Override // documentation inherited
    public int getTicksPerMove ()
    {
        return 2;
    }

    @Override // documentation inherited
    public String getLogic ()
    {
        return "com.threerings.bang.game.server.ai.WhiteStagLogic";
    }
}
