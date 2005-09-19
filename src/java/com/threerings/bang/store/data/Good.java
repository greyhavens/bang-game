//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangUserObject;

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
     * Returns a fully qualified translatable string indicating the name
     * of this good.
     */
    public String getName ()
    {
        return MessageBundle.qualify(BangCodes.GOODS_MSGS, "m." + _type);
    }

    /**
     * Returns a fully qualified translatable string used to convey
     * additional information about the good in question.
     */
    public abstract String getTip ();

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

    /**
     * Indicates that this good is available to the specified user.
     */
    public abstract boolean isAvailable (BangUserObject user);

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        return _type;
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return _type.hashCode();
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return other.getClass().equals(getClass()) &&
            _type.equals(((Good)other)._type);
    }

    /** Creates a good of the specified type. */
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
