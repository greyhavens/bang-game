//
// $Id$

package com.threerings.bang.game.data.effect;

import java.awt.Point;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Duplicates a piece.
 */
public class DuplicateEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String DUPLICATED = "bonuses/duplicate/activate";

    /** Reported when a duplicate could not be placed for lack of room. */
    public static final String WASTED_DUP = "wasted_dup";

    public int pieceId;

    public Piece duplicate;

    @Override // documentation inherited
    public void init (Piece piece)
    {
        pieceId = piece.pieceId;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId, duplicate.pieceId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        Unit unit = (Unit)bangobj.pieces.get(pieceId);
        if (unit == null) {
            return;
        }

        // find a place to put our new unit
        Point spot = bangobj.board.getOccupiableSpot(unit.x, unit.y, 2);
        if (spot == null) {
            log.info("Dropped duplicate effect. No spots [unit=" + unit + "].");
            return;
        }

        // position our new unit
        duplicate = unit.duplicate(bangobj);
        duplicate.position(spot.x, spot.y);

        // update the board shadow to reflect its future existence
        bangobj.board.updateShadow(null, duplicate);
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        Piece piece = (Piece)bangobj.pieces.get(pieceId);
        if (piece == null) {
            return;
        }

        // report wastage if we were unable to place the new piece
        if (duplicate == null) {
            reportEffect(obs, piece, WASTED_DUP);
            return;
        }

        // inform the observer of our duplication
        reportEffect(obs, piece, DUPLICATED);

        // and add the new piece, informing the observer again
        bangobj.addPieceDirect(duplicate);
        reportAddition(obs, duplicate);
    }
}
