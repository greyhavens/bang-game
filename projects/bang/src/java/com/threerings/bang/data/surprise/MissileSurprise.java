//
// $Id$

package com.threerings.bang.data.surprise;

import com.threerings.bang.data.effect.AreaDamageEffect;
import com.threerings.bang.data.effect.Effect;

/**
 * A surprise that allows the player to launch a missile that does an area
 * of effect damage.
 */
public class MissileSurprise extends Surprise
{
    public int power = 60;

    public int radius = 2;

    @Override // documentation inherited
    public String getIconPath ()
    {
        return "missile" + radius;
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return radius;
    }

    @Override // documentation inherited
    public Effect activate (int x, int y)
    {
        return new AreaDamageEffect(power, getRadius(), x, y);
    }
}
