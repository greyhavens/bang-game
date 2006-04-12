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
                // TODO: pull the whole luck thing out into something way more
                // complicated so that we can report when you get a 2x hit
                return (RandomUtil.getInt(100) >= 50) ? 2 * damage : damage;
            }
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "bonuses/lady_luck/activate";
    }
}
