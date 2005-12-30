//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.game.data.ScenarioCodes;

/**
 * Contains the player's ratings and experience for a particular game scenario.
 */
public class Rating
    implements DSet.Entry
{
    /** The default rating value. */
    public static final int DEFAULT_RATING = 1200;

    /** The scenario for which this rating applies (or {@link
     * ScenarioCodes#OVERALL} for the player's overall rating. */
    public String scenario;

    /** The actual rating value. */
    public int rating;

    /** The number of rounds of the scenario, (or games in total for the
     * overall rating) the player has played. */
    public int experience;

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        return scenario;
    }
}
