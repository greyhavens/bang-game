//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.piece.Hindrance;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.client.effect.InfluenceViz;
import com.threerings.bang.game.client.effect.IconInfluenceViz;

/**
 * An effect that causes a steam unit to take damage at every step.
 */
public class MonkeyWrenchEffect extends SetHindranceEffect
{
    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return super.isApplicable() &&
            _unit.getConfig().make == UnitConfig.Make.STEAM;
    }

    @Override // documentation inherited
    protected Hindrance createHindrance (final Unit target)
    {
        return new Hindrance() {
            public String getName () {
                return "monkey_wrench";
            }
            public InfluenceViz createViz (boolean high) {
                return new IconInfluenceViz("monkey_wrench");
            }
            public Effect maybeGeneratePostMoveEffect (int steps) {
                return new DamageEffect(target, steps * DAMAGE_PER_STEP);
            }
            public boolean isVisible () {
                return true;
            }
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "boom_town/monkey_wrench";
    }

    /** The amount of damage to inflict per step. */
    protected static final int DAMAGE_PER_STEP = 5;

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.BonusEffect.class, "bonusId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.game.data.effect.BonusEffect.class, "pieceId", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.BonusEffect.class, "bonusId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.game.data.effect.BonusEffect.class, "pieceId", this, out);
    }
}
