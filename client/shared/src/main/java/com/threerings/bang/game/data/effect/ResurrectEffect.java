//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * Resurrects a single unit at half health and converts them temporarily to the
 * resurrector's team.
 */
public class ResurrectEffect extends Effect
{
    /** An effect reported on resurrected units. */
    public static final String RESURRECTED = "boom_town/dust_devil";

    /** The identifier of the piece to be resurrected. */
    public int pieceId;

    /** The index of the resurrecting player. */
    public int resurrector;

    public ResurrectEffect ()
    {
    }

    public ResurrectEffect (int pieceId, int resurrector)
    {
        this.pieceId = pieceId;
        this.resurrector = resurrector;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // nothing doing
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null) {
            log.warning("Missing target for resurrect effect", "id", pieceId);
            return false;
        }

        piece.setOwner(bangobj, resurrector);
        piece.damage = 50;
        piece.lastActed = (short)(bangobj.tick - 4);
        reportEffect(obs, piece, RESURRECTED);
        return true;
    }
}
