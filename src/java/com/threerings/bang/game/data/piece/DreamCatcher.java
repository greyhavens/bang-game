//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.ShotEffect;

/**
 * Handles the special abilities of the Dream Catcher.
 */
public class DreamCatcher extends Unit
{
    @Override // documentation inherited
    public ShotEffect shoot (BangObject bangobj, Piece target, float scale)
    {
        // she does no damage
        ShotEffect shot = super.shoot(bangobj, target, 0f);
        // but she resets the target's tick counter
        if (target.lastActed < bangobj.tick) {
            shot.targetLastActed = bangobj.tick;
        }
        return shot;
    }
}
