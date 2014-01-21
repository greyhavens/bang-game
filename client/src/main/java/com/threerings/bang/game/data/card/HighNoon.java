//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.HighNoonEffect;

/**
 * Temporarily gives a movement penalty to all units.
 */
public class HighNoon extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "high_noon";
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.FRONTIER_TOWN;
    }

    @Override // documentation inherited
    public PlacementMode getPlacementMode ()
    {
        return PlacementMode.VS_BOARD;
    }

    @Override // documentation inherited
    public boolean isValid (BangObject bangobj)
    {
        return true;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 25;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 0;
    }

    @Override // documenataion inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        return new HighNoonEffect();
    }
}
