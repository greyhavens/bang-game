//
// $Id$

package com.threerings.bang.game.data.effect;

import com.samskivert.util.IntIntMap;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Homestead;
import com.threerings.bang.game.data.piece.Piece;

import static com.threerings.bang.Log.*;

/**
 * Deployed when a player claims a Homestead in the Land Grab scenario.
 */
public class HomesteadEffect extends Effect
{
    /** The identifier for the type of effect that we produce. */
    public static final String STEAD_CLAIMED =
        "frontier_town/homestead/claimed";

    /** The piece id of the claiming piece. */
    public int claimerId;
    
    /** The piece id of the homestead being claimed. */
    public int steadId;

    @Override // documentation inherited
    public void init (Piece claimer)
    {
        claimerId = claimer.pieceId;
    }
    
    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { steadId };
    }

    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        return new int[] { claimerId };
    }
    
    @Override // documentation inherited
    public void prepare (BangObject bangobj, IntIntMap dammap)
    {
        // nothing to do here
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        Piece claimer = bangobj.pieces.get(claimerId);
        if (claimer == null) {
            log.warning("Missing claimer for homestead effect", "pieceId", claimerId);
            return false;
        }
        Homestead stead = (Homestead)bangobj.pieces.get(steadId);
        stead.setOwner(bangobj, claimer.owner);
        reportEffect(obs, stead, STEAD_CLAIMED);
        return true;
    }
}
