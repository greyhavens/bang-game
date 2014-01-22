//
// $Id$

package com.threerings.bang.data;

/**
 * A temporary handle for an unnamed player.
 */
public class GuestHandle extends Handle
{
    /** Set the default name for a guest handle. */
    public static void setDefaultName (String name)
    {
        _defaultName = name;
    }

    /**
     * Creates a guest handle instance with the supplied unique hash.
     */
    public GuestHandle (String hash)
    {
        super(hash);
    }

    @Override // documentation inherited
    public String toString ()
    {
        return _defaultName;
    }

    protected static String _defaultName = "Guest Handle";
}
