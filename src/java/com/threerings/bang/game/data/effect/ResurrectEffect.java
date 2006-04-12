//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Resurrects a single unit at half health and converts them temporarily to the
 * resurrector's team.
 */
public class ResurrectEffect extends AreaEffect
{
    /** An effect reported on resurrected units. */
    public static final String RESURRECTED = "resurrected";

    /** The index of the resurrecting player. */
    public int resurrector;

    public ResurrectEffect ()
    {
    }

    public ResurrectEffect (int resurrector, int x, int y)
    {
        super(0, x, y);
        this.resurrector = resurrector;
    }

    @Override // documentation inherited
    protected boolean affectedPiece (Piece piece)
    {
        // we only work on dead pieces
        return (piece instanceof Unit && piece.owner >= 0 && !piece.isAlive());
    }

    @Override // documentation inherited
    protected void apply (
        BangObject bangobj, Observer obs, int pidx, Piece piece, int dist)
    {
        piece.owner = resurrector;
        piece.damage = 50;
        piece.lastActed = (short)(bangobj.tick - 4);
        reportEffect(obs, piece, RESURRECTED);
    }
}
