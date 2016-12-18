//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Expires the currently active hindrance on a unit.
 */
public class ExpireHindranceEffect extends ExpireInfluenceEffect
{
    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        Unit unit = (Unit)bangobj.pieces.get(pieceId);
        if (unit != null) {
            unit.setHindrance(null);
            reportEffect(obs, unit, UPDATED);
        }
        return true;
    }
}
