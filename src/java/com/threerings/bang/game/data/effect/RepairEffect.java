//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.log;

/**
 * An effect that repairs a particular piece on the board.
 */
public class RepairEffect extends BonusEffect
{
    /** The identifier for the type of effect that we produce. */
    public static final String REPAIRED = "bonuses/repair/activate";

    /** The identifier of the piece to be repaired. */
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
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        Piece piece = bangobj.pieces.get(pieceId);
        if (piece == null) {
            log.warning("Missing target for repair effect " +
                        "[id=" + pieceId + "].");
            return false;
        }

        piece.damage = 0;
        reportEffect(obs, piece, REPAIRED);
        return true;
    }
}
