//
// $Id$

package com.threerings.bang.data;

import com.threerings.presents.dobj.DSet;
import com.threerings.util.Name;

/**
 * An entry in the list of pardners.
 */
public class PardnerEntry
    implements DSet.Entry, Comparable
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
    
    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        return handle;
    }
    
    // documentation inherited from interface Comparable
    public int compareTo (Object other)
    {
        return handle.compareTo(((PardnerEntry)other).handle);
    }
}
