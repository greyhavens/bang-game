//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Adds a underdog soldier to the board.
 */
public class AddUnderdogSoldierEffect extends AddPieceEffect
{
    public AddUnderdogSoldierEffect (int x, int y)
    {
        piece = Unit.getUnit("indian_post/underdog_soldier");
        piece.position(x, y);
    }

    public AddUnderdogSoldierEffect ()
    {
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        if (bangobj.board.isOccupiable(piece.x, piece.y)) {
            piece.assignPieceId(bangobj);
            super.prepare(bangobj, dammap);
        } else {
            piece = null;
        }
    }

    @Override // documentation inherited
    public boolean isApplicable ()
    {
        return piece != null;
    }

    @Override // documentation inherited
    public String getDescription (BangObject bangobj, int pidx)
    {
        return "m.effect_underdog_soldier";
    }
}
