//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.StringUtil;

import com.threerings.bang.data.UnitConfig;
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

    public int activator;

    public int[] pieceIds = new int[0];

    @Override // documentation inherited
    public void init (Piece piece)
    {
        activator = piece.owner;
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // determine which of our opponents is the strongest and steal one
        // of their pieces
        int maxpower = Integer.MIN_VALUE, pidx = -1;
        for (int ii = 0; ii < bangobj.pstats.length; ii++) {
            if (ii == activator) {
                continue;
            }
            if (bangobj.pstats[ii].power > maxpower) {
                pidx = ii;
                maxpower = bangobj.pstats[ii].power;
            }
        }
        if (pidx == -1) {
            log.warning("Failed to find player for defect " +
                        "[activator=" + activator + ", pstats=" +
                        StringUtil.toString(bangobj.pstats) + "].");
            return;
        }

        // now steal a random non-bigshot unit from this player
        Piece[] pieces = bangobj.getPieceArray();
        ArrayUtil.shuffle(pieces);
        for (int ii = 0; ii < pieces.length; ii++) {
            if (pieces[ii].owner != pidx || !(pieces[ii] instanceof Unit)) {
                continue;
            }
            Unit unit = (Unit)pieces[ii];
            if (unit.getConfig().rank == UnitConfig.Rank.BIGSHOT) {
                continue;
            }
            pieceIds = new int[] { unit.pieceId };
            break;
        }
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
            p.owner = activator;
            reportEffect(obs, p, DEFECTED);
            defected++;
        }
        if (defected > 0) {
            // the balance of power has shifted, recompute our stats
            bangobj.updateStats();
        }
    }
}
