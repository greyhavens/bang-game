//
// $Id$

package com.threerings.bang.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.piece.Piece;

/**
 * An effect that clears out all dead units within a certain area.
 */
public class AreaClearEffect extends AreaEffect
{
    /** The identifier for the type of effect that we produce. */
    public static final String CLEARED = "cleared";

    public AreaClearEffect ()
    {
    }

    public AreaClearEffect (int radius, int x, int y)
    {
        super(radius, x, y);
    }

    @Override // documentation inherited
    protected void noteAffected (Piece piece, IntIntMap dammap)
    {
        // NOOP
    }

    @Override // documentation inherited
    protected boolean affectedPiece (Piece piece)
    {
        return (piece.owner >= 0 && !piece.isAlive());
    }

    @Override // documentation inherited
    protected void apply (
        BangObject bangobj, Observer obs, Piece piece, int dist)
    {
        // first report that the piece is being cleared
        reportEffect(obs, piece, CLEARED);

        // then actually clear it from the board
        bangobj.pieces.removeDirect(piece);
        bangobj.board.updateShadow(piece, null);
        reportRemoval(obs, piece);
    }
}
