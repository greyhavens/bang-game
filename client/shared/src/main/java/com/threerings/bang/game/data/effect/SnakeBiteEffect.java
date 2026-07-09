//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.data.UnitConfig;

import com.threerings.bang.game.data.piece.Hindrance;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.client.effect.IconInfluenceViz;
import com.threerings.bang.game.client.effect.InfluenceViz;

/**
 * An effect that causes a human unit to take damage at every step.
 */
public class SnakeBiteEffect extends SetHindranceEffect
{
    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return super.isApplicable() &&
            _unit.getConfig().make == UnitConfig.Make.HUMAN;
    }

    @Override // documentation inherited
    protected Hindrance createHindrance (final Unit target)
    {
        return new Hindrance() {
            public String getName () {
                return "snake_bite";
            }
            public InfluenceViz createViz (boolean high) {
                return new IconInfluenceViz("snake_bite");
            }
            public Effect tick () {
                return new DamageEffect(target, DAMAGE_PER_TICK);
            }
            public boolean isVisible () {
                return true;
            }
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "snake_bite";
    }
    
    /** The amount of damage to inflict per tick. */
    protected static final int DAMAGE_PER_TICK = 5;

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
