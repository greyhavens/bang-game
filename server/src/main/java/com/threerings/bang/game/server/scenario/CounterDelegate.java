//
// $Id$

package com.threerings.bang.game.server.scenario;

import java.awt.Point;
import java.util.List;

import com.google.common.collect.Lists;

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
        // find all the counters on this board
        List<Counter> counters = Lists.newArrayList();
        for (Piece piece : bangobj.getPieceArray()) {
            if (piece instanceof Counter) {
                counters.add((Counter)piece);
            }
        }

        // assign counters to start positions
        for (int ii = 0; ii < bangobj.players.length; ii++) {
            Point start = _parent.getStartSpot(ii);

            int mindist = Integer.MAX_VALUE;
            Counter counter = null;
            for (Counter c : counters) {
                int dist = c.getDistance(start.x, start.y);
                if (dist < mindist) {
                    mindist = dist;
                    counter = c;
                }
            }
            if (counter == null) {
                throw new InvocationException("m.no_start_marker_for_counter");
            }
            counters.remove(counter);

            // configure this counter for play
            counter.owner = ii;
            counter.count = startingCount();
            bangobj.updatePieces(counter);

            // start the player with points for each nugget
            int points = counter.count * pointsPerCounter();
            bangobj.grantPoints(ii, points);
            _counters.add(counter);
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
    protected List<Counter> _counters = Lists.newArrayList();
}
