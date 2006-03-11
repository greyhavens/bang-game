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
    /** The new owner of the cow. */
    public int owner = -1;

    /** The piece id of the spooker (used for animation purposes). */
    public int spookerId;

    @Override // documentation inherited
    public int[] getAffectedPieces ()
    {
        return new int[] { pieceId, spookerId };
    }

    @Override // documentation inherited
    public void apply (BangObject bangobj, Observer obs)
    {
        // update the cow's owner (if appropriate), then let them move
        if (owner != -1) {
            Cow cow = (Cow)bangobj.pieces.get(pieceId);
            if (cow != null) {
                cow.owner = owner;
            }
        }
        super.apply(bangobj,obs);
    }
}
