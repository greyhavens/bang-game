//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.client.effect.IconInfluenceViz;
import com.threerings.bang.game.client.effect.InfluenceViz;
import com.threerings.bang.game.client.effect.ParticleInfluenceViz;

import com.threerings.bang.game.data.piece.Hindrance;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Causes a unit to take damage every tick.
 */
public class OnFireEffect extends SetHindranceEffect
{
    // the player index for the unit which caused the fire
    public int pidx;

    public OnFireEffect (Piece piece, int pidx)
    {
        init(piece);
        this.pidx = pidx;
    }

    /**
     * For unserialization.
     */
    public OnFireEffect ()
    {
    }

    @Override // documentation inherited
    protected Hindrance createHindrance (final Unit target)
    {
        return new Hindrance() {
            public String getName () {
                return "on_fire";
            }
            public InfluenceViz createViz (boolean high) {
                return (high ?
                        new ParticleInfluenceViz("frontier_town/fire") :
                        new IconInfluenceViz("on_fire"));
            }
            public Effect tick () {
                return new DamageEffect(target, DAMAGE_PER_TICK, pidx);
            }
            public boolean isVisible () {
                return true;
            }
            protected int duration () {
                return FIRE_DURATION;
            }
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "indian_post/on_fire";
    }

    /** The amount of damage to inflict per tick. */
    protected static final int DAMAGE_PER_TICK = 5;

    /** The number of ticks the fire lasts. */
    protected static final int FIRE_DURATION = 4;
}
