//
// $Id$

package com.threerings.bang.game.data.effect;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.piece.Cow;

/**
 * A move effect for a cow that also changes its ownership.
 */
public class SpookEffect extends MoveEffect
{
    /** An effect reported when a cow is branded. */
    public static final String BRANDED = "frontier_town/cow/branded";

    /** An effect reported when a cow is merely spooked. */
    public static final String SPOOKED = "frontier_town/cow/spooked";

    /** The new owner of the cow. */
    public int owner = -1;

    /** The piece id of the spooker (used for animation purposes). */
    public int spookerId;

    @Override // documentation inherited
    public int[] getWaitPieces ()
    {
        return new int[] { spookerId };
    }

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId };
    }

    @Override // documentation inherited
    public boolean apply (BangObject bangobj, Observer obs)
    {
        // update the cow's owner (if appropriate), then let them move
        Cow cow = (Cow)bangobj.pieces.get(pieceId);
        if (cow != null) {
            String effect = SPOOKED;
            if (owner != -1 && cow.owner != owner) {
                cow.setOwner(bangobj, owner);
                effect = BRANDED;
            }
            // report an effect on the cow so that we can play a sound
            reportEffect(obs, cow, effect);
        }
        return super.apply(bangobj,obs);
    }
}
