//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Unit;
import com.threerings.bang.game.data.piece.WhiteStag;

/**
 * Adds a white stag to the board.
 */
public class AddWhiteStagEffect extends AddPieceEffect
{
    public AddWhiteStagEffect (int x, int y)
    {
        piece = Unit.getUnit("indian_post/white_stag");
        piece.position(x, y);
    }

    public AddWhiteStagEffect ()
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
        return "m.effect_white_stag";
    }
}
