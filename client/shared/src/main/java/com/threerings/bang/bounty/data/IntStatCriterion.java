//
// $Id$

package com.threerings.bang.bounty.data;

import java.io.IOException;
import java.util.HashSet;

import com.jme.util.export.InputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.OutputCapsule;

import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.StatType;

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
    public StatType stat;

    /** The condition to be met. */
    public Condition condition;

    /** The value against which to compare the stat. */
    public int value;

    // from Criterion
    public String getDescription ()
    {
        return MessageBundle.compose(
                "m." + StringUtil.toUSLowerCase(condition.toString()) + "_descrip",
                stat.key(), MessageBundle.taint(String.valueOf(value)));
    }

    // from Criterion
    public void addWatchedStats (HashSet<StatType> stats)
    {
        stats.add(stat);
    }

    @Override // from Criterion
    public State getCurrentState (BangObject bangobj, int rank)
    {
        // TODO: support COMPLETED, FAILED
        return isMet(bangobj, rank) ? State.MET : State.NOT_MET;
    }

    // from Criterion
    public String getCurrentValue (BangObject bangobj, int rank)
    {
        return MessageBundle.taint(String.valueOf(bangobj.critStats.getIntStat(stat)));
    }

    // from Criterion
    public boolean isMet (BangObject bangobj, int rank)
    {
        int actual = (bangobj.critStats == null) ? 0 : bangobj.critStats.getIntStat(stat);
        switch (condition) {
        case LESS_THAN:
            if (actual < value) {
                return true;
            }
            break;
        case EQUAL_TO:
            if (actual == value) {
                return true;
            }
            break;
        case AT_LEAST:
            if (actual >= value) {
                return true;
            }
            break;
        }
        return false;
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
        stat = StatType.valueOf(in.readString("stat", null));
        condition = Condition.valueOf(in.readString("condition", null));
        value = in.readInt("value", 0);
    }

    // from interface Savable
    public Class<?> getClassTag ()
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
}
