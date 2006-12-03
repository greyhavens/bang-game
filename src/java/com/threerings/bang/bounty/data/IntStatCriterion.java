//
// $Id$

package com.threerings.bang.bounty.data;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.Criterion;

/**
 * Requires that a particular integer stat be less than, greater than or equal to a value.
 */
public class IntStatCriterion extends Criterion
{
    /** Defines the various supported conditions. */
    public enum Condition { LESS_THAN, AT_LEAST, EQUAL_TO };

    /** The statistic in question. */
    public Stat.Type stat;

    /** The condition to be met. */
    public Condition condition;

    /** The value against which to compare the stat. */
    public int value;

    // from Criterion
    public String isMet (BangObject bangobj, PlayerObject player)
    {
        int pidx = bangobj.getPlayerIndex(player.handle);
        int actual = bangobj.stats[pidx].getIntStat(stat);
        switch (condition) {
        case LESS_THAN:
            if (actual < value) {
                return null;
            }
            break;
        case EQUAL_TO:
            if (actual == value) {
                return null;
            }
            break;
        case AT_LEAST:
            if (actual >= value) {
                return null;
            }
            break;
        }
        String msg = MessageBundle.compose("m." + condition.toString().toLowerCase() + "_failed",
                                           stat.key(), MessageBundle.taint(String.valueOf(value)),
                                           MessageBundle.taint(String.valueOf(actual)));
        return MessageBundle.qualify(OfficeCodes.OFFICE_MSGS, msg);
    }

    @Override // from Object
    public String toString ()
    {
        return stat + " " + condition + " " + value;
    }
}
