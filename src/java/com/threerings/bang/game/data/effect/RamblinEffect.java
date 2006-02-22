//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Influence;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * An effect that causes the piece in question to ramble along and move one
 * square further until it is killed and respawned.
 */
public class RamblinEffect extends Effect
{
    /** The influence we have on units. */
    public static class RamblinInfluence extends Influence
    {
        @Override // documentation inherited
        public int adjustMoveDistance (int moveDistance) {
            return moveDistance+1;
        }
    }

    /** The identifier for the type of effect that we produce. */
    public static final String RAMBLIN = "bonuses/ramblin/activate";

    /** The piece we will affect. */
    public int pieceId;

    @Override // documentation inherited
    public void init (Piece piece)
    {
        pieceId = piece.pieceId;
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
    public void apply (BangObject bangobj, Observer obs)
    {
        Unit unit = (Unit)bangobj.pieces.get(pieceId);
        if (unit == null) {
            return;
        }

        unit.setInfluence(new RamblinInfluence(), bangobj.tick);
        reportEffect(obs, unit, RAMBLIN);
    }
}
