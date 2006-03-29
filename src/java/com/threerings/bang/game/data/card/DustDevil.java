//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.AreaClearEffect;
import com.threerings.bang.game.data.effect.Effect;

/**
 * A card that allows the player to clear out all dead pieces in an area.
 */
public class DustDevil extends Card
{
    public int radius = 2;

    @Override // documentation inherited
    public String getType ()
    {
        return "dust_devil";
    }

    @Override // documentation inherited
    public void init (BangObject bangobj, int owner)
    {
        super.init(bangobj, owner);

        // TODO: change radius for any reason?
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return radius;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 10;
    }

    @Override // documentation inherited
    public Effect activate (int x, int y)
    {
        return new AreaClearEffect(getRadius(), x, y);
    }
}
