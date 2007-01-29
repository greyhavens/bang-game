//
// $Id$

package com.threerings.bang.data;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.StringUtil;

/**
 * Used to track a statistic comprised of a set of strings.
 */
public abstract class StringSetStat extends Stat
{
    /**
     * Returns true if the specified string is contained in this set.
     */
    public boolean contains (String key)
    {
        return ArrayUtil.binarySearch(_values, 0, _values.length, key) >= 0;
    }

    /**
     * Adds the specified string to this set.
     *
     * @return true if the string was newly added, false if it was already
     * contained in the set.
     */
    public boolean add (String key)
    {
        if (key == null) {
            throw new IllegalArgumentException("Cannot add null to StringSetStat");
        }

        int iidx = ArrayUtil.binarySearch(_values, 0, _values.length, key);
        if (iidx >= 0) {
            return false;
        }

        iidx = -iidx - 1;
        String[] values = new String[_values.length+1];
        System.arraycopy(_values, 0, values, 0, iidx);
        System.arraycopy(_values, iidx, values, iidx+1, (_values.length-iidx));
        values[iidx] = key;
        _values = values;
        _modified = true;
        return true;
    }

    @Override // documentation inherited
    public String valueToString ()
    {
        return StringUtil.toString(_values);
    }

    /** Contains the strings in this set. */
    protected String[] _values = new String[0];
}
