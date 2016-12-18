//
// $Id$

package com.threerings.bang.game.data.effect;

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

    /** Add a secondary affected piece if we want this effect to wait on the
     * completion of another effect. */
    public int queuePiece = -1;

    /**
     * Creates a counter effect configured to update the count on a counter.
     */
    public static CountEffect changeCount (int id, int count)
    {
        return changeCount (id, count, -1);
    }

    /**
     * Creates a counter effect configured to update the count on a counter.
     */
    public static CountEffect changeCount (int id, int count, int queuePiece)
    {
        CountEffect effect = new CountEffect();
        effect.counterId = id;
        effect.newCount = count;
        effect.queuePiece = queuePiece;
        return effect;
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        if (queuePiece != -1) {
            return new int[] { counterId, queuePiece };
        }
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
