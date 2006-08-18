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
    protected ShotEffect unitShoot (
            BangObject bangobj, Piece target, float scale)
    {
        ShotEffect shot = super.unitShoot(bangobj, target, scale);
        // the codger resets the tick counter of the unit he fires upon
        if (target.lastActed < bangobj.tick) {
            shot.targetLastActed = bangobj.tick;
        }
        return shot;
    }
}
