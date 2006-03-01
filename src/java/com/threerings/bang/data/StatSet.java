//
// $Id$

package com.threerings.bang.data;

import java.util.Iterator;

import com.threerings.presents.dobj.DSet;

/**
 * A distributed set containing {@link Stat} objects.
 */
public final class StatSet extends DSet
{
    /** Creates a stat set with the specified contents. */
    public StatSet (Iterator<Stat> contents)
    {
        super(contents);
    }

    /** Creates a blank stat set. */
    public StatSet ()
    {
    }

    /**
     * Wires this stat set up to a containing user object. All subsequent
     * modifications will be published to the container.
     */
    public void setContainer (PlayerObject container)
    {
        _container = container;
    }

    /**
     * Sets an integer statistic in this set.
     *
     * @exception ClassCastException thrown if the registered type of the
     * specified stat is not an {@link IntStat}.
     */
    public void setStat (Stat.Type type, int value)
    {
        IntStat stat = (IntStat)get(type.name());
        if (stat == null) {
            stat = (IntStat)type.newStat();
            stat.setValue(value);
            addStat(stat);
        } else if (stat.setValue(value)) {
            updateStat(stat);
        }
    }

    /**
     * Sets an integer stat to the specified value, if it exceeds our
     * existing recorded value.
     *
     * @exception ClassCastException thrown if the registered type of the
     * specified stat is not an {@link IntStat}.
     */
    public void maxStat (Stat.Type type, int value)
    {
        int ovalue = getIntStat(type);
        if (value > ovalue) {
            setStat(type, value);
        }
    }

    /**
     * Increments an integer statistic in this set.
     *
     * @exception ClassCastException thrown if the registered type of the
     * specified stat is not an {@link IntStat}.
     */
    public void incrementStat (Stat.Type type, int delta)
    {
        IntStat stat = (IntStat)get(type.name());
        if (stat == null) {
            stat = (IntStat)type.newStat();
            stat.increment(delta);
            addStat(stat);
        } else if (stat.increment(delta)) {
            updateStat(stat);
        }
    }

    /**
     * Adds a string value to a {@link StringSetStat}.
     *
     * @exception ClassCastException thrown if the registered type of the
     * specified stat is not an {@link StringSetStat}.
     */
    public void addToSetStat (Stat.Type type, String value)
    {
        StringSetStat stat = (StringSetStat)get(type.name());
        if (stat == null) {
            stat = (StringSetStat)type.newStat();
            stat.add(value);
            addStat(stat);
        } else if (stat.add(value)) {
            updateStat(stat);
        }
    }

    /**
     * Returns the current value of the specified integer statistic.
     */
    public int getIntStat (Stat.Type type)
    {
        IntStat stat = (IntStat)get(type.name());
        return (stat == null) ? 0 : stat.getValue();
    }

    /**
     * Returns true if the specified {@link StringSetStat} contains the
     * specified value, false otherwise.
     */
    public boolean containsValue (Stat.Type type, String value)
    {
        StringSetStat stat = (StringSetStat)get(type.name());
        return (stat == null) ? false : stat.contains(value);
    }

    protected void addStat (Stat stat)
    {
        if (_container != null) {
            _container.addToStats(stat);
        } else {
            add(stat);
        }
    }

    protected void updateStat (Stat stat)
    {
        if (_container != null) {
            _container.updateStats(stat);
        }
    }

    protected transient PlayerObject _container;
}
