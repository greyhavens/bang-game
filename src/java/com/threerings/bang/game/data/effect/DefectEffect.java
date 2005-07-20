//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.StringUtil;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

import static com.threerings.bang.Log.log;

/**
 * Converts some number of units from the other players to the activating
 * players control.
 */
public class DefectEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String DEFECTED = "defected";

    public int owner;

    public int[] pieceIds;

    @Override // documentation inherited
    public void init (Piece piece)
    {
        owner = piece.owner;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // for each player that has at least three more units than we do,
        // we take one unit out of every three they have in excess
        int[] ucount = bangobj.getUnitCount();
        int[] scount = new int[ucount.length];
        for (int ii = 0; ii < ucount.length; ii++) {
            scount[ii] = Math.max(ucount[ii] - ucount[owner], 0) / 3;
        }

        log.info("Defecting [owner=" + owner +
                 ", ucount=" + StringUtil.toString(ucount) +
                 ", scount=" + StringUtil.toString(scount) + "].");

        // now steal whatever remains
        ArrayIntSet pids = new ArrayIntSet();
        Piece[] pieces = bangobj.getPieceArray();
        ArrayUtil.shuffle(pieces);

        // make a first pass, trying not to steal artillery
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (isValidSteal(p, false) && scount[p.owner] > 0) {
                scount[p.owner]--;
                pids.add(p.pieceId);
            }
        }

        // make a second pass, allowing artillery if we didn't find enough
        // the first time through
        for (int ii = 0; ii < pieces.length; ii++) {
            Piece p = pieces[ii];
            if (isValidSteal(p, true) && scount[p.owner] > 0 &&
                !pids.contains(p.pieceId)) {
                scount[p.owner]--;
                pids.add(p.pieceId);
            }
        }

        pieceIds = pids.toIntArray();
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        // swipe away!
        int defected = 0;
        for (int ii = 0; ii < pieceIds.length; ii++) {
            Piece p = (Piece)bangobj.pieces.get(pieceIds[ii]);
            if (p == null || !p.isAlive()) {
                continue;
            }
            p.owner = owner;
            reportEffect(obs, p, DEFECTED);
            defected++;
        }
        if (defected > 0) {
            // the balance of power has shifted, recompute our stats
            bangobj.updateStats();
        }
    }

    // TODO: nix this artillery crap
    protected boolean isValidSteal (Piece p, boolean allowArtillery)
    {
        return p.owner >= 0 && p.isAlive() && p.owner != owner &&
            (allowArtillery || !(p instanceof Unit) ||
             !(((Unit)p).getType().equals("artillery")));
    }
}
