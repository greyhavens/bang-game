//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.effect.AdjustTickEffect;
import com.threerings.bang.game.data.effect.Effect;

/**
 * A card that allows the player to move a unit again immediately.
 */
public class GiddyUp extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "giddy_up";
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return 0;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 40;
    }

    @Override // documentation inherited
    public Effect activate (int x, int y)
    {
        return new AdjustTickEffect(x, y, -4);
    }
}
