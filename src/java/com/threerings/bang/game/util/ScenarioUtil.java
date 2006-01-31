//
// $Id$

package com.threerings.bang.game.util;

import java.util.HashMap;
import java.util.Iterator;

import com.threerings.util.RandomUtil;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.ScenarioCodes;
import com.threerings.bang.game.data.piece.Claim;
import com.threerings.bang.game.data.piece.Cow;
import com.threerings.bang.game.data.piece.Piece;

/**
 * Contains scenario-related utilities.
 */
public class ScenarioUtil
{
    /**
     * Computes the unscored but in-progress cash for each of the players,
     * which tends to be scenario dependent: nuggets in claims, branded cattle,
     * etc. Adds it to the supplied funds array.
     */
    public static void computeUnscoredFunds (BangObject bangobj, int[] funds)
    {
        // it'd be nice to have the scenario do this but it's a server side
        // class and this method needs to be called every time a piece moves
        // which would be a pesky amount of computation to do on the server
        if (bangobj.scenarioId.equals(ScenarioCodes.CLAIM_JUMPING)) {
            // add the cash from nuggets in claims
            for (Iterator iter = bangobj.pieces.iterator(); iter.hasNext(); ) {
                Piece p = (Piece)iter.next();
                if (p.owner >= 0 && p instanceof Claim) {
                    Claim c = (Claim)p;
                    funds[c.owner] += c.nuggets * ScenarioCodes.CASH_PER_NUGGET;
                }
            }

        } else if (bangobj.scenarioId.equals(ScenarioCodes.CATTLE_RUSTLING)) {
            // add the cash from branded cattle
            for (Iterator iter = bangobj.pieces.iterator(); iter.hasNext(); ) {
                Object p = iter.next();
                if (p instanceof Cow) {
                    Cow c = (Cow)p;
                    if (c.owner >= 0) {
                        funds[c.owner] += ScenarioCodes.CASH_PER_COW;
                    }
                }
            }
        }
    }

    /**
     * Selects a random list of scenarios for the specified town.
     */
    public static String[] selectRandom (String townId, int count)
    {
        String[] avail = _scenmap.get(townId);
        String[] choices = new String[count];
        for (int ii = 0; ii < choices.length; ii++) {
            choices[ii] = (String)RandomUtil.pickRandom(avail);
        }
        return choices;
    }

    /** Maps town ids to a list of valid gameplay scenarios. */
    protected static HashMap<String,String[]> _scenmap =
        new HashMap<String,String[]>();
    static {
        _scenmap.put(BangCodes.FRONTIER_TOWN,
                     ScenarioCodes.FRONTIER_TOWN_SCENARIOS);
        _scenmap.put(BangCodes.INDIAN_VILLAGE,
                     ScenarioCodes.INDIAN_VILLAGE_SCENARIOS);
        _scenmap.put(BangCodes.BOOM_TOWN,
                     ScenarioCodes.BOOM_TOWN_SCENARIOS);
        _scenmap.put(BangCodes.GHOST_TOWN,
                     ScenarioCodes.GHOST_TOWN_SCENARIOS);
        _scenmap.put(BangCodes.CITY_OF_GOLD,
                     ScenarioCodes.CITY_OF_GOLD_SCENARIOS);
    }
}
