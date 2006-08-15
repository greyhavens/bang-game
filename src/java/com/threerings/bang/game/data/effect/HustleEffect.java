//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that causes the piece in question to hustle up and move in one
 * fewer ticks than normal until it is killed and respawned.
 */
public class HustleEffect extends SetInfluenceEffect
{
    @Override // documentation inherited
    protected Influence createInfluence (Unit target)
    {
        return new Influence() {
            public String getName () {
                return "hustle";
            }
            public int adjustTicksPerMove (int ticksPerMove) {
                return ticksPerMove-1;
            }
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "frontier_town/hustle";
    }
}
