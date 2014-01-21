//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that causes the piece in question to adjust it's move distance
 * for some number of ticks, or until it is killed and respawned. 
 */
public class AdjustMoveInfluenceEffect extends SetInfluenceEffect
{
    /** The influence icon. */
    public String icon;

    /** The effect name. */
    public String name;

    /** The amount the move will change by. */
    public int moveDelta;

    /** The number of ticks before expiring (or -1 to never expire). */
    public int expireTicks;

    public AdjustMoveInfluenceEffect ()
    {
    }

    public AdjustMoveInfluenceEffect (int moveDelta, int expireTicks)
    {
        this.moveDelta = moveDelta;
        this.expireTicks = expireTicks;
    }
    
    @Override // documentation inherited
    protected Influence createInfluence (Unit target)
    {
        return new Influence() {
            public String getName () {
                return icon;
            }
            public int adjustMoveDistance (int moveDistance) {
                return moveDistance + moveDelta;
            }
            public int duration () {
                if (expireTicks > -1) {
                    return expireTicks;
                }
                return super.duration();
            }
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return name;
    }

}
