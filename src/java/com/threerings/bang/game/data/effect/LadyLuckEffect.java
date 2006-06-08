//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.util.RandomUtil;

import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that causes the piece in question to become "lucky" which gives it
 * a chance of doing 2x damage on future attacks.
 */
public class LadyLuckEffect extends SetInfluenceEffect
{
    @Override // documentation inherited
    protected Influence createInfluence (Unit target)
    {
        return new Influence() {
            public String getIcon () {
                return "lady_luck";
            }
            public int adjustAttack (Piece target, int damage) {
                _didAdjustAttack = RandomUtil.getInt(100) >= 50;
                return _didAdjustAttack ? 2 * damage : damage;
            }
            public boolean didAdjustAttack () {
                return _didAdjustAttack;
            }

            protected boolean _didAdjustAttack = false;
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "bonuses/frontier_town/lady_luck/activate";
    }
}
