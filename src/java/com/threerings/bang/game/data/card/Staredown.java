//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.StaredownEffect;

/**
 * A card that allows the player to delay by one tick the action of any
 * piece on the board.
 */
public class Staredown extends Card
{
    @Override // documentation inherited
    public String getIconPath ()
    {
        return "staredown";
    }

    @Override // documentation inherited
    public int getRadius ()
    {
        return 1;
    }

    @Override // documentation inherited
    public Effect activate (int x, int y)
    {
        return new StaredownEffect(x, y);
    }
}
