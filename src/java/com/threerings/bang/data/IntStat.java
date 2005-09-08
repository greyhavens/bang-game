//
// $Id$

package com.threerings.bang.data;

/**
 * Used to track a single integer statistic.
 */
public class IntStat extends Stat
{
    /**
     * Returns the value of this integer statistic.
     */
    public int getValue ()
    {
        return _value;
    }

    /**
     * Sets this statistic's value to the specified value.
     *
     * @return true if the stat was modified, false if not.
     */
    public boolean setValue (int value)
    {
        if (value != _value) {
            _value = value;
            _modified = true;
            return true;
        }
        return false;
    }

    /**
     * Increments this statistic by the specified delta value.
     *
     * @return true if the stat was modified, false if not.
     */
    public boolean increment (int delta)
    {
        return setValue(_value + delta);
    }

    @Override // documentation inherited
    public String valueToString ()
    {
        return String.valueOf(_value);
    }

    /** Contains the integer value of this statistic. */
    protected int _value;
}
