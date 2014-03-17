//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.RepairAllEffect;

/**
 * A card that heals all living units in the game.
 */
public class Forgiven extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "forgiven";
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.INDIAN_POST;
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
        return 10;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 0;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        return new RepairAllEffect();
    }
}
