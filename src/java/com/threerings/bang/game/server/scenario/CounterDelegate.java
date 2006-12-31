//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.util.ArrayList;

import com.samskivert.util.ArrayIntSet;

import com.threerings.presents.server.InvocationException;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Counter;
import com.threerings.bang.game.data.piece.Piece;
import com.threerings.bang.game.data.piece.Unit;

/**
 * Handles scenarios with counter props.
 */
public abstract class CounterDelegate extends ScenarioDelegate
{
    @Override // documentation inherited
    public void roundWillStart (BangObject bangobj)
        throws InvocationException
    {
        ArrayIntSet assigned = new ArrayIntSet();
        Piece[] pieces = bangobj.getPieceArray();
        for (int ii = 0; ii < pieces.length; ii++) {
            if (!(pieces[ii] instanceof Counter)) {
                continue;
            }

            // determine which start marker to which it is nearest
            Counter counter = (Counter)pieces[ii];
            int midx = _parent.getOwner(counter, assigned);
            if (midx == -1) {
                throw new InvocationException("m.no_start_marker_for_counter");
            }

            // make sure we have a player associated with this start marker
            if (midx >= bangobj.players.length) {
                continue;
            }

            // configure this counter for play
            counter.owner = midx;
            counter.count = startingCount();
            bangobj.updatePieces(counter);

            // start the player with points for each nugget
            int points = counter.count * pointsPerCounter();
            bangobj.grantPoints(midx, points);
            _counters.add(counter);
            assigned.add(midx);
        }
    }

    @Override // documentation inherited
    public void pieceMoved (BangObject bangobj, Piece piece)
    {
        if (piece instanceof Unit) {
            checkAdjustedCounter(bangobj, (Unit)piece);
        }
    }

    /**
     * The starting count for each counter.
     */
    protected int startingCount ()
    {
        return 0;
    }

    /**
     * The amount of points each count on the counter is worth.
     */
    protected abstract int pointsPerCounter ();

    /**
     * Checks whether any counters should be incremented or decremented due to the specified unit
     * which has just moved.
     */
    protected abstract void checkAdjustedCounter (BangObject bangobj, Unit unit);

    /** A list of the active counters. */
    protected ArrayList<Counter> _counters = new ArrayList<Counter>();
}
