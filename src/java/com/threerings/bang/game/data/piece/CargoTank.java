//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.NuggetEffect;

/**
 * Holds players' nuggets in the Gold Rush scenario.
 */
public class CargoTank extends Counter
{
    @Override // documentation inherited
    public int getGoalRadius (BangObject bangobj, Piece mover)
    {
        return (mover.owner == owner && mover instanceof Unit &&
            NuggetEffect.isNuggetBonus(((Unit)mover).holding)) ? +1 : -1;
    }
}
