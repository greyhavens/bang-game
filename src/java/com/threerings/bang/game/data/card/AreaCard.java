//
// $Id$

package com.threerings.bang.game.data.card;

import com.threerings.media.util.MathUtil;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Provides default implementations for Area effect cards.
 */
public abstract class AreaCard extends Card
{
    @Override // documentation inherited
    public PlacementMode getPlacementMode ()
    {
        return PlacementMode.VS_AREA;
    }

    @Override // documentation inherited
    public boolean isValidLocation (BangObject bangobj, int tx, int ty)
    {
        int r2 = getRadius() * getRadius();
        for (Piece p : bangobj.pieces) {
            if (p instanceof Unit && p.isAlive() && 
                    MathUtil.distanceSq(p.x, p.y, tx, ty) <= r2) {
                return true;
            }
        }
        return false;
    }
}
