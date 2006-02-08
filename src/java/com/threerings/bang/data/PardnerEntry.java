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
    /** The pardner is not logged in. */
    public static final byte OFFLINE = 0;
    
    /** The pardner is in the town interface. */
    public static final byte IN_TOWN = 1;
    
    /** The pardner is in the bank interface. */
    public static final byte IN_BANK = 2;
    
    /** The pardner is in the ranch interface. */
    public static final byte IN_RANCH = 3;
    
    /** The pardner is in the store interface. */
    public static final byte IN_STORE = 4;
    
    /** The pardner is in the saloon interface. */
    public static final byte IN_SALOON = 5;
    
    /** The pardner is in the barber interface. */
    public static final byte IN_BARBER = 6;
    
    /** The pardner is in a game. */
    public static final byte IN_GAME = 7;
    
    /** The pardner's handle. */
    public Name handle;
    
    /** The pardner's avatar. */
    public int[] avatar;
    
    /** The pardner's status ({@link #OFFLINE}, {@link #IN_TOWN}, etc). */
    public byte status;
    
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
     * Determines whether this pardner is online.
     */
    public boolean isOnline ()
    {
        return status != OFFLINE;
    }
    
    /**
     * Determines whether this pardner is available for chat (i.e., online and
     * not in a game).
     */
    public boolean isAvailable ()
    {
        return status != OFFLINE && status != IN_GAME;
    }
    
    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        return handle;
    }
    
    // documentation inherited from interface Comparable
    public int compareTo (Object other)
    {
        // sort online pardners above offline ones and available ones above
        // unavailable ones
        PardnerEntry oentry = (PardnerEntry)other;
        if (isOnline() != oentry.isOnline()) {
            return isOnline() ? -1 : +1;
        
        } else if (isAvailable() != oentry.isAvailable()) {
            return isAvailable() ? -1 : +1;
            
        } else {
            return handle.compareTo(oentry.handle);
        }
    }
}
