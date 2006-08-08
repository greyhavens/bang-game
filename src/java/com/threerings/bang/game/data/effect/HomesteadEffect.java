//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Homestead;

/**
 * Deployed when a player claims a Homestead in the Land Grab scenario.
 */
public class HomesteadEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String STEAD_CLAIMED =
        "effects/frontier_town/stead_claimed/activate";

    /** The piece id of the homestead being claimed. */
    public int steadId;

    /** The new owner for the homestead. */
    public int owner;

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { steadId };
    }

    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // nothing to do here
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        Homestead stead = (Homestead)bangobj.pieces.get(steadId);
        stead.owner = owner;
        reportEffect(obs, stead, STEAD_CLAIMED);
        return true;
    }
}
