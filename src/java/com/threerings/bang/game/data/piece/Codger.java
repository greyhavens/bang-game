//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.ShotEffect;

/**
 * Handles the special capabilities of the Codger unit.
 */
public class Codger extends Unit
{
    @Override // documentation inherited
    public ShotEffect shoot (BangObject bangobj, Piece target)
    {
        ShotEffect shot = super.shoot(bangobj, target);
        // the codger adds a tick to the unit he fires upon
        if (target.lastActed < bangobj.tick) {
            shot.newLastActed = (short)(target.lastActed + 1);
        }
        return shot;
    }
}
