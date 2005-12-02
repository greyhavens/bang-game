//
// $Id$

package com.threerings.bang.game.util;

import java.util.Iterator;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.piece.Claim;
import com.threerings.bang.game.data.piece.Cow;

/**
 * Contains scenario-related utilities.
 */
public class ScenarioUtil
{
    /**
     * Computes the unscored but in-progress cash for each of the players,
     * which tends to be scenario dependent: nuggets in claims, branded cattle,
     * etc.
     */
    public static void computeUnscoredFunds (BangObject bangobj, int[] funds)
    {
        // it'd be nice to have the scenario do this but it's a server side
        // class and this method needs to be called every time a piece moves
        // which would be a pesky amount of computation to do on the server
        if (bangobj.scenarioId.equals(GameCodes.CLAIM_JUMPING)) {
            // add the cash from nuggets in claims
            for (Iterator iter = bangobj.pieces.iterator(); iter.hasNext(); ) {
                Object p = iter.next();
                if (p instanceof Claim) {
                    Claim c = (Claim)p;
                    funds[c.owner] += c.nuggets * GameCodes.CASH_PER_NUGGET;
                }
            }

        } else if (bangobj.scenarioId.equals(GameCodes.CATTLE_HERDING)) {
            // add the cash from branded cattle
            for (Iterator iter = bangobj.pieces.iterator(); iter.hasNext(); ) {
                Object p = iter.next();
                if (p instanceof Cow) {
                    Cow c = (Cow)p;
                    if (c.owner >= 0) {
                        funds[c.owner] += GameCodes.CASH_PER_COW;
                    }
                }
            }
        }
    }
}
