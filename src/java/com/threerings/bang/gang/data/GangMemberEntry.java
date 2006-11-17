//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.bang.data.PardnerEntry;

/**
 * Extends {@link PardnerEntry} with gang-related data.
 */
public class GangMemberEntry extends PardnerEntry
{
    /** The member's gang rank. */
    public byte rank;
    
    /**
     * No-arg constructor for deserialization.
     */
    public GangMemberEntry ()
    {
    }
}
