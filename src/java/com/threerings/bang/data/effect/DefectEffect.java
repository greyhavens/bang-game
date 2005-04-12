//
// $Id$

package com.threerings.bang.data.effect;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntIntMap;

import com.threerings.bang.data.BangObject;
import com.threerings.bang.data.piece.Piece;

import java.util.Iterator;

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

    public DefectEffect (int owner)
    {
        this.owner = owner;
    }

    public DefectEffect ()
    {
    }

    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // determine the probability with which we swipe units (from 0.1 to 0.6)
        double swipeChance =
            (1.0 - Math.min(bangobj.pstats[owner].powerFactor, 1.0))/2 + 0.1;
        log.info(bangobj.players[owner] + " swiping with probability " +
                 swipeChance);

        ArrayIntSet pieces = new ArrayIntSet();
        for (Iterator iter = bangobj.pieces.iterator(); iter.hasNext(); ) {
            Piece p = (Piece)iter.next();
            if (p.isAlive() && p.owner >= 0 && p.owner != owner &&
                Math.random() < swipeChance) {
                pieces.add(p.pieceId);
            }
        }

        pieceIds = pieces.toIntArray();
    }

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
}
