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

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.ConsolidatedOffer.class, "volume", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.data.ConsolidatedOffer.class, "price", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.ConsolidatedOffer.class, "volume", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.data.ConsolidatedOffer.class, "price", this, out);
    }
}
