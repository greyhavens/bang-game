//
// $Id$

package com.threerings.bang.game.data.piece;

import com.threerings.bang.game.data.effect.BallisticShotEffect;
import com.threerings.bang.game.data.effect.ShotEffect;

/**
 * Does something extraordinary.
 */
public class BallisticUnit extends Unit
{
    @Override // documentation inherited
    protected ShotEffect generateShotEffect (Piece target, int damage)
    {
        return new BallisticShotEffect(this, target, damage,
                attackInfluenceIcon(), defendInfluenceIcon(target));
    }
}
