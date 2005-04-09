//
// $Id$

package com.samskivert.bang.data.surprise;

import com.samskivert.bang.data.effect.AreaDamageEffect;
import com.samskivert.bang.data.effect.Effect;

/**
 * A surprise that allows the player to launch a missile that does an area
 * of effect damage.
 */
public class MissileSurprise extends Surprise
{
    @Override // documentation inherited
    public String getIconPath ()
    {
        return "missile";
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return 3;
    }

    @Override // documentation inherited
    public Effect activate (int x, int y)
    {
        return new AreaDamageEffect(80, getRadius(), x, y);
    }
}
