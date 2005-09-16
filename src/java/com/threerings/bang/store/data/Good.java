//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

/**
 * Represents a particular good that can be purchased from the general
 * store.
 */
public abstract class Good extends SimpleStreamableObject
    implements DSet.Entry
{
    /** A constructor only used during serialization. */
    public Good ()
    {
    }

    /**
     * Returns the string identifier that uniquely identifies this type of
     * good.
     */
    public String getType ()
    {
        return _type;
    }

    /**
     * Returns the cost of this good in gold coins. This is in addition to
     * the scrip cost ({@link #getScripCost}).
     */
    public int getGoldCost ()
    {
        return _goldCost;
    }

    /**
     * Returns the cost of this good in scrip. This is in addition to the
     * gold cost ({@link #getGoldCost}).
     */
    public int getScripCost ()
    {
        return _scripCost;
    }

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        return _type;
    }

    /**
     * Creates a good of the specified type.
     */
    protected Good (String type, int goldCost, int scripCost)
    {
        _type = type;
        _goldCost = goldCost;
        _scripCost = scripCost;
    }

    protected String _type;
    protected int _goldCost;
    protected int _scripCost;
}
