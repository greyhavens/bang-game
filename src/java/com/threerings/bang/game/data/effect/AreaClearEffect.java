//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that clears out all dead units within a certain area.
 */
public class AreaClearEffect extends AreaEffect
{
    /** The identifier for the type of effect that we produce. */
    public static final String CLEARED =
        "cards/frontier_town/dust_devil/activate";

    public AreaClearEffect ()
    {
    }

    public AreaClearEffect (int radius, int x, int y)
    {
        super(radius, x, y);
    }

    @Override // documentation inherited
    protected boolean isPieceAffected (Piece piece)
    {
        return (piece instanceof Unit && !piece.isAlive());
    }

    @Override // documentation inherited
    protected void apply (
        BangObject bangobj, Observer obs, int pidx, Piece piece, int dist)
    {
        // first report that the piece is being cleared
        reportEffect(obs, piece, CLEARED);

        // then actually clear it from the board
        bangobj.removePieceDirect(piece);
        reportRemoval(obs, piece);
    }
}
