//
// $Id$

package com.threerings.bang.gang.data;

import java.util.Date;

import com.threerings.io.SimpleStreamableObject;

/**
 * An entry in the gang's historical log.
 */
public class HistoryEntry extends SimpleStreamableObject
{
    /** The time at which the event occurred. */
    public long recorded;
    
    /** The translatable description of the event. */
    public String description;
    
    /**
     * Constructor for entries created from the database.
     */
    public HistoryEntry (Date recorded, String description)
    {
        this.recorded = recorded.getTime();
        this.description = description;
    }
    
    /**
     * No-arg constructor for deserialization.
     */
    public HistoryEntry ()
    {
    }
    
    /**
     * Returns the time at which the event occurred as a {@link Date} instance.
     */
    public Date getRecordedDate ()
    {
        return new Date(recorded);
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.HistoryEntry.class, "recorded", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.gang.data.HistoryEntry.class, "description", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.HistoryEntry.class, "recorded", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.gang.data.HistoryEntry.class, "description", this, out);
    }
}
