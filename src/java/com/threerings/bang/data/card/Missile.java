//
// $Id$

package com.threerings.bang.data.surprise;

import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.effect.AreaDamageEffect;
import com.threerings.bang.data.effect.Effect;

/**
 * A surprise that allows the player to launch a missile that does an area
 * of effect damage.
 */
public class Missile extends Surprise
{
    public int power = 60;

    public int radius = 2;

    @Override // documentation inherited
    public void init (BangObject bangobj, int owner)
    {
        super.init(bangobj, owner);

        // if our player is "in the nooksak", give them a big missile
        if (bangobj.pstats[owner].power < 30) {
            power = 100;
            radius = 4;
        } else if (bangobj.pstats[owner].powerFactor < 0.34) {
            power = 80;
            radius = 3;
        }
    }

    @Override // documentation inherited
    public String getIconPath ()
    {
        return "missile";
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
