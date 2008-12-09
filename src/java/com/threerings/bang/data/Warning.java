//
// $Id$

package com.threerings.bang.data;

import com.threerings.util.MessageBundle;

/**
 * Represents a warning to a player.
 */
public class Warning extends Notification
{
    /** A warning type. */
    public static final String TEMP_BAN = "m.warning_tb";

    /** A warning type. */
    public static final String WARNING = "m.warning";

    /** The type of warning. */
    public String type;

    /** The message associated to this warning. */
    public String message;

    /**
     * Creates a new warning message on the server.
     */
    public Warning (String type, String message, ResponseHandler handler)
    {
        super(handler);
        this.type = type;
        this.message = message;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Warning ()
    {
    }

    // documentation inherited from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return "WARNING" + message;
    }

    @Override // documentation inherited
    public String getTitle ()
    {
        return type + "_title";
    }

    @Override // documentation inherited
    public String getText ()
    {
        return MessageBundle.tcompose(type + "_info", message);
    }

    @Override // documentation inherited
    public String [] getResponses ()
    {
        return new String[] { type + "_button" };
    }

    @Override // documentation inherited
    public int getEnabledDelay ()
    {
        return BUTTON_DELAY;
    }

    protected static final int BUTTON_DELAY = 30;
}
