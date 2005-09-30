//
// $Id$

package com.threerings.bang.data;

import com.threerings.io.SimpleStreamableObject;

/**
 * Contains consolidated information on all coin exchange offers at a
 * particular price.
 */
public class ConsolidatedOffer extends SimpleStreamableObject
    implements Comparable<ConsolidatedOffer>
{
    /** The number of coins being offered to buy or sell. */
    public int volume;

    /** The price per coin. */
    public int price;

    // documentation inherited from interface Comparable
    public int compareTo (ConsolidatedOffer other)
    {
        return (price - other.price);
    }
}
