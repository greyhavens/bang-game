//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.BuckleInfo;
import com.threerings.bang.data.Handle;

/**
 * Contains a list of top-ranked gangs according to some criterion.
 */
public class TopRankedGangList extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The criterion for this ranking. */
    public String criterion;

    /** The names of the gangs in rank order. */
    public Handle[] names;

    /** The buckle of the number one gang. */
    public BuckleInfo topDogBuckle;

    // documentation inherited from interface DSet.Key
    public Comparable<?> getKey ()
    {
        return criterion;
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.TopRankedGangList.class, "criterion", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.TopRankedGangList.class, "names", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.TopRankedGangList.class, "topDogBuckle", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.TopRankedGangList.class, "criterion", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.TopRankedGangList.class, "names", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.TopRankedGangList.class, "topDogBuckle", this, out);
    }
}
