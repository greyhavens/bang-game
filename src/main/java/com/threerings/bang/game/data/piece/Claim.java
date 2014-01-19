//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.NuggetEffect;

/**
 * Holds players' nuggets in the Claim Jumping scenario.
 */
public class Claim extends Counter
{
    @Override // documentation inherited
    public int getGoalRadius (BangObject bangobj, Piece mover)
    {
        return (mover instanceof Unit &&
            ((mover.owner != owner && count > 0 &&
                ((Unit)mover).holding == null) ||
            (mover.owner == owner && NuggetEffect.isNuggetBonus(
                ((Unit)mover).holding)))) ? +1 : -1;
    }
}
