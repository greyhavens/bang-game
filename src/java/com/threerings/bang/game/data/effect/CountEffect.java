//
// $Id$

package com.threerings.bang.game.data.effect;

import java.util.Observer;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Counter;

/**
 * Causes the count to change on a {@link Counter}.
 */
public class CountEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String COUNT_CHANGED = "count_changed";

    /** The id of the counter being changed. */
    public int counterId;

    /** The new count for the counter. */
    public int newCount;

    /**
     * Creates a counter effect configured to update the count on a counter.
     */
    public static CountEffect changeCount (int id, int count)
    {
        CountEffect effect = new CountEffect();
        effect.counterId = id;
        effect.newCount = count;
        return effect;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { counterId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // nothing to do here
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        Counter counter = (Counter)bangobj.pieces.get(counterId);
        counter.count = newCount;
        reportEffect(obs, counter, COUNT_CHANGED);
        return true;
    }
}
