//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that gives a 30% attack bonus across the board to the piece in
 * question.
 */
public class PowerUpEffect extends SetInfluenceEffect
{
    @Override // documentation inherited
    protected Influence createInfluence (Unit target)
    {
        return new Influence() {
            public String getName () {
                return "power_up";
            }
            public int adjustAttack (Piece target, int damage) {
                return Math.round(1.3f * damage);
            }
            public boolean didAdjustAttack () {
                return true;
            }
            public boolean showClientAdjust () {
                return true;
            }
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "frontier_town/power_up";
    }
}
