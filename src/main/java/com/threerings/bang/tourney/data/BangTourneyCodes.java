//
// $Id$

package com.threerings.bang.tourney.data;

import com.threerings.parlor.tourney.data.TourneyCodes;

/**
 * Constants for bang tournaments.
 */
public interface BangTourneyCodes extends TourneyCodes
{
    /** Minimum player options. */
    public static final int[] MIN_PLAYERS = {2, 4, 8, 16, 32};

    /** Possible values for tournament start times. */
    public static final int[] STARTS_IN = {2, 5, 10, 15, 30, 60};
}
