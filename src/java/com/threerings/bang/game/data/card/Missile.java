//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AreaDamageEffect;
import com.threerings.bang.game.data.effect.Effect;

/**
 * A card that allows the player to launch a missile that does an area of
 * effect damage.
 */
public class Missile extends AreaCard
{
    @Override // documentation inherited
    public String getType ()
    {
        return "missile";
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return RADIUS;
    }

    @Override // documentation inherited
    public String getTownId ()
    {
        return BangCodes.FRONTIER_TOWN;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 30;
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 40;
    }

    @Override // documentation inherited
    public Effect activate (BangObject bangobj, Object target)
    {
        int[] coords = (int[])target;
        return new AreaDamageEffect(
            owner, POWER, getRadius(), coords[0], coords[1]);
    }

    protected static final int POWER = 60;
    protected static final int RADIUS = 1;
}
