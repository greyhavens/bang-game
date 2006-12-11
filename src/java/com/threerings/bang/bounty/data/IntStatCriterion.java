//
// $Id$

package com.threerings.bang.bounty.data;

import java.io.IOException;

import com.jme.util.export.InputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.OutputCapsule;

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
        return createMessage(bangobj, player, "failed");
    }

    // from Criterion
    public String reportMet (BangObject bangobj, PlayerObject player)
    {
        return createMessage(bangobj, player, "met");
    }

    // from interface Savable
    public void write (JMEExporter ex) throws IOException
    {
        OutputCapsule out = ex.getCapsule(this);
        out.write(stat.toString(), "stat", null);
        out.write(condition.toString(), "condition", null);
        out.write(value, "value", 0);
    }

    // from interface Savable
    public void read (JMEImporter im) throws IOException
    {
        InputCapsule in = im.getCapsule(this);
        stat = Stat.Type.valueOf(in.readString("stat", null));
        condition = Condition.valueOf(in.readString("condition", null));
        value = in.readInt("value", 0);
    }

    // from interface Savable
    public Class getClassTag ()
    {
        return getClass();
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        IntStatCriterion ocrit = (IntStatCriterion)other;
        return stat == ocrit.stat && condition == ocrit.condition && value == ocrit.value;
    }

    @Override // from Object
    public String toString ()
    {
        return stat + " " + condition + " " + value;
    }

    protected String createMessage (BangObject bangobj, PlayerObject player, String type)
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
        String msg = MessageBundle.compose("m." + condition.toString().toLowerCase() + "_" + type,
                                           stat.key(), MessageBundle.taint(String.valueOf(value)),
                                           MessageBundle.taint(String.valueOf(actual)));
        return MessageBundle.qualify(OfficeCodes.OFFICE_MSGS, msg);
    }
}
