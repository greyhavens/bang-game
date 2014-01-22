//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.Collections;
import java.util.List;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.StringUtil;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * An effect that replaces all units in excess of the per-player average
 * with 40% health windup gunslingers owned by the original player.
 */
public class PlagueEffect extends BonusEffect
{
    /** The identifier for the type of effect that we produce. */
    public static final String PLAGUED = "ghost_town/plague";

    public int owner;
    public int[] pieceIds;
    public Piece[] newPieces;

    @Override // documentation inherited
    public void init (Piece piece)
    {
        super.init(piece);
        owner = piece.owner;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return pieceIds;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        super.prepare(bangobj, dammap);

        // everyone gets to keep the "average" count or at least two
        // pieces, whichever is higher
        int save = Math.max(2, bangobj.getAverageUnitCount());

        // subtract off the "reserved" count from each player
        int[] ucount = bangobj.getUnitCount();
        for (int ii = 0; ii < ucount.length; ii++) {
            ucount[ii] = Math.max(0, ucount[ii] - save);
        }

        log.info("Plaguing", "avg", save, "ucount", StringUtil.toString(ucount),
                 "ocount", StringUtil.toString(bangobj.getUnitCount()));

        // determine which pieces will be affected
        ArrayIntSet pids = new ArrayIntSet();
        List<Piece> pieces = bangobj.getPieceArray();
        Collections.shuffle(pieces);
        for (Piece p : pieces) {
            if (p.owner >= 0 && p.isAlive() && ucount[p.owner] > 0 &&
                // make sure we don't try to turn a dirigible over a
                // building or water into a windup gunman
                bangobj.board.isGroundOccupiable(p.x, p.y)) {
                ucount[p.owner]--;
                pids.add(p.pieceId);
            }
        }

        pieceIds = pids.toIntArray();
        newPieces = new Piece[pieceIds.length];
        for (int ii = 0; ii < newPieces.length; ii++) {
            newPieces[ii] = Unit.getUnit("windupslinger");
            newPieces[ii].init();
            newPieces[ii].assignPieceId(bangobj);
            newPieces[ii].setOwner(bangobj,
                bangobj.pieces.get(pieceIds[ii]).owner);
            newPieces[ii].damage = 40;
        }
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        super.apply(bangobj, obs);

        // remove the old pieces and add new windup gun slingers instead
        for (int ii = 0; ii < pieceIds.length; ii++) {
            Piece p = bangobj.pieces.get(pieceIds[ii]);
            if (p == null) {
                continue;
            }
            reportEffect(obs, p, PLAGUED);
            removeAndReport(bangobj, p, obs);

            newPieces[ii].position(p.x, p.y);
            addAndReport(bangobj, newPieces[ii], obs);
        }

        // the balance of power has shifted, recompute our metrics
        bangobj.updateData();

        return true;
    }
}
