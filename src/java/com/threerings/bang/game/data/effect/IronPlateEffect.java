//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that causes the piece in question to become invincible for seven
 * ticks.
 */
public class IronPlateEffect extends SetInfluenceEffect
{
    @Override // documentation inherited
    protected Influence createInfluence (Unit target)
    {
        return new Influence() {
            public String getIcon () {
                return "iron_plate";
            }
            public int adjustDefend (Piece shooter, int damage) {
                return 0;
            }
            public boolean didAdjustDefend () {
                return true;
            }
            protected int duration () {
                return 7;
            }
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "bonuses/frontier_town/iron_plate/activate";
    }
}
