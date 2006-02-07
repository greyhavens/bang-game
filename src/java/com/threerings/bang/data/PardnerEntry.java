//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.dobj.DSet;
import com.threerings.util.Name;

/**
 * An entry in the list of pardners.
 */
public class PardnerEntry
    implements DSet.Entry
{
    /** The pardner's handle. */
    public Name handle;
    
    /**
     * No-arg constructor for deserialization.
     */
    public PardnerEntry ()
    {
    }
    
    /**
     * Standard constructor.
     */
    public PardnerEntry (Name handle)
    {
        this.handle = handle;
    }
    
    /**
     * Returns the key used to identify this item in a {@link DSet}.
     */
    public Comparable getKey ()
    {
        return handle;
    }
}
