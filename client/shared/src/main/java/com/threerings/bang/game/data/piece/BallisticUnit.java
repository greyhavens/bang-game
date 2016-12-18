//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.BallisticShotEffect;
import com.threerings.bang.game.data.effect.ShotEffect;

/**
 * The superclass of units that fire arcing ballistic shots.
 */
public class BallisticUnit extends Unit
{
    @Override // documentation inherited
    protected ShotEffect generateShotEffect (
            BangObject bangobj, Piece target, int damage)
    {
        return new BallisticShotEffect(this, target, damage,
                attackInfluenceIcons(), defendInfluenceIcons(target));
    }
}
