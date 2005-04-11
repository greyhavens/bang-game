//
// $Id$

package com.samskivert.bang.data.surprise;

import com.samskivert.bang.data.effect.AreaRepairEffect;
import com.samskivert.bang.data.effect.Effect;

/**
 * A surprise that allows the player to repair a group of pieces.
 */
public class RepairSurprise extends Surprise
{
    public int repair = 60;

    public int radius = 2;

    @Override // documentation inherited
    public String getIconPath ()
    {
        return "repair" + radius;
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return radius;
    }

    @Override // documentation inherited
    public Effect activate (int x, int y)
    {
        return new AreaRepairEffect(repair, getRadius(), x, y);
    }
}
