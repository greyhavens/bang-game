//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that causes the piece in question to ramble along and move one
 * square further until it is killed and respawned.
 */
public class RamblinEffect extends SetInfluenceEffect
{
    @Override // documentation inherited
    protected Influence createInfluence (Unit target)
    {
        return new Influence() {
            public String getIcon () {
                return "ramblin";
            }
            public int adjustMoveDistance (int moveDistance) {
                return moveDistance+1;
            }
        };
    }

    @Override // documentation inherited
    protected String getEffectName ()
    {
        return "bonuses/ramblin/activate";
    }
}
