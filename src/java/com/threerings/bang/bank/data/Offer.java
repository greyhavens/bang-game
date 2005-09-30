//
// $Id$

package com.threerings.bang.bank.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

/**
 * Contains information on a coin exchange offer.
 */
public class Offer extends SimpleStreamableObject
    implements DSet.Entry
{
    /** A unique identifier for this offer. */
    public int offerId;

    /** The number of coins being offered to buy or sell. */
    public int volume;

    /** The price per coin. */
    public int price;

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        if (_key == null) {
            _key = new Integer(offerId);
        }
        return _key;
    }

    protected transient Integer _key;
}
