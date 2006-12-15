//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;

/**
 * Represents a particular good that can be purchased from the general
 * store.
 */
public abstract class Good extends SimpleStreamableObject
    implements DSet.Entry, Comparable<Good>
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
     * Returns the filename of the icon associated with this good. The default
     * is based on the type of the good, but this can be overridden by
     * specialized goods.
     */
    public String getIconPath ()
    {
        return "goods/" + _type + ".png";
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
     * Returns a fully qualified translatable string used to convey
     * additional information about the good in question for a tooltip window.
     */
    public String getToolTip ()
    {
        return getTip();
    }

    /**
     * Returns the cost of this good in scrip. This is in addition to the
     * coin cost ({@link #getCoinCost}).
     */
    public int getScripCost ()
    {
        return _scripCost;
    }

    /**
     * Returns the cost of this good in coins. This is in addition to the scrip
     * cost ({@link #getScripCost}).
     */
    public int getCoinCost ()
    {
        return _coinCost;
    }

    /**
     * Returns the coin transaction type to use when tracking the purchase of this good.
     */
    public int getCoinType ()
    {
        return CoinTransaction.GOOD_PURCHASE;
    }

    /**
     * Indicates that this good is available to the specified user.
     */
    public abstract boolean isAvailable (PlayerObject user);

    // from interface DSet.Entry
    public Comparable getKey ()
    {
        return _type;
    }

    // from interface Comparable<Good>
    public int compareTo (Good other)
    {
        return _type.compareTo(other._type);
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
    protected Good (String type, int scripCost, int coinCost)
    {
        _type = type;
        _scripCost = scripCost;
        _coinCost = coinCost;
    }

    protected String _type;
    protected int _scripCost;
    protected int _coinCost;
}
