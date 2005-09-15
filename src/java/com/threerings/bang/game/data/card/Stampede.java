//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;

/**
 * Causes a herd of buffalo to stampede across the board, damaging any
 * units in their path.
 */
public class Stampede extends Card
{
    @Override // documentation inherited
    public void init (BangObject bangobj, int owner)
    {
        super.init(bangobj, owner);

        // TODO
    }

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
    public Effect activate (int x, int y)
    {
        return null; // TODO
    }
}
