//
// $Id$

package com.threerings.bang.bounty.data;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.game.data.BangObject;

/**
 * Requires that a particular integer stat be less than, greater than or equal to a value.
 */
public class IntStatCriterion extends Criterion
{
    /** Defines the various supported conditions. */
    public enum Condition { LESS_THAN, MORE_THAN, EQUAL_TO };

    /** The statistic in question. */
    public Stat.Type stat;

    /** The condition to be met. */
    public Condition condition;

    /** The value against which to compare the stat. */
    public int value;

    // from Criterion
    public boolean isMet (BangObject bangobj, PlayerObject player)
    {
        return false;
    }
}
