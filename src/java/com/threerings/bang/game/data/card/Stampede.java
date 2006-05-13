//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.StampedeEffect;

/**
 * Causes a herd of buffalo to stampede across the board, damaging any
 * units in their path.
 */
public class Stampede extends Card
{
    @Override // documentation inherited
    public String getType ()
    {
        return "stampede";
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return 1;
    }

    @Override // documentation inherited
    public int getWeight ()
    {
        return 30;
    }

    @Override // documentation inherited
    public Effect activate (int x, int y)
    {
        return new StampedeEffect(owner, x, y, getRadius());
    }

    @Override // documentation inherited
    public int getScripCost ()
    {
        return 300;
    }

    @Override // documentation inherited
    public int getCoinCost ()
    {
        return 1;
    }
}
