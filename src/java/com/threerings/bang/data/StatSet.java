//
// $Id$

package com.threerings.bang.data;

import java.util.Iterator;

import com.threerings.presents.dobj.DObject;
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
    public void setContainer (BangUserObject container)
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
            if (_container != null) {
                _container.updateStats(stat);
            } else {
                update(stat);
            }
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
        } else {
            update(stat);
        }
    }

    protected transient BangUserObject _container;
}
