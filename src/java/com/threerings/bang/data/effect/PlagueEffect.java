//
// $Id$

package com.threerings.bang.data.effect;

import java.util.Iterator;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.StringUtil;

import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.data.piece.WindupSlinger;

import static com.threerings.bang.Log.log;

/**
 * An effect that replaces all units in excess of the per-player average
 * with 40% health windup gunslingers owned by the original player.
 */
public class PlagueEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String PLAGUED = "plagued";

    public int owner;
    public int[] pieceIds;
    public Piece[] newPieces;

    public PlagueEffect (int owner)
    {
        this.owner = owner;
    }

    public PlagueEffect ()
    {
    }

    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // everyone gets to keep the "average" count or at least two
        // pieces, whichever is higher
        int save = Math.max(2, bangobj.getAveragePieceCount());

        // subtract off the "reserved" count from each player
        int[] pcount = bangobj.getPieceCount();
        for (int ii = 0; ii < pcount.length; ii++) {
            pcount[ii] = Math.max(0, pcount[ii] - save);
        }

        log.info("Plaguing [avg=" + save +
                 ", pcount=" + StringUtil.toString(pcount) +
                 ", ocount=" + StringUtil.toString(bangobj.getPieceCount()) +
                 "].");

        // determine which pieces will be affected
        ArrayIntSet pids = new ArrayIntSet();
        Piece[] pieces = bangobj.getPieceArray();
        ArrayUtil.shuffle(pieces);

        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (p.owner >= 0 && p.isAlive() && pcount[p.owner] > 0 &&
                // make sure we don't try to turn a dirigible over a
                // building or water into a windup gunman
                bangobj.board.isGroundOccupiable(p.x, p.y)) {
                pcount[p.owner]--;
                pids.add(p.pieceId);
            }
        }

        pieceIds = pids.toIntArray();
        newPieces = new Piece[pieceIds.length];
        for (int ii = 0; ii < newPieces.length; ii++) {
            newPieces[ii] = new WindupSlinger();
            newPieces[ii].init();
            newPieces[ii].assignPieceId();
            newPieces[ii].owner =
                ((Piece)bangobj.pieces.get(pieceIds[ii])).owner;
            newPieces[ii].damage = 40;
        }
    }

    public void apply (BangObject bangobj, Observer obs)
    {
        // remove the old pieces and add new windup gun slingers instead
        for (int ii = 0; ii < pieceIds.length; ii++) {
            Piece p = (Piece)bangobj.pieces.get(pieceIds[ii]);
            if (p == null) {
                continue;
            }
            bangobj.removePieceDirect(p);
            reportEffect(obs, p, PLAGUED);
            reportRemoval(obs, p);

            newPieces[ii].position(p.x, p.y);
            bangobj.addPieceDirect(newPieces[ii]);
            reportAddition(obs, newPieces[ii]);
        }

        // the balance of power has shifted, recompute our stats
        bangobj.updateStats();
    }
}
