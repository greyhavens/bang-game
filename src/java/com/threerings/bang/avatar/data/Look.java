//
// $Id$

package com.threerings.bang.avatar.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

/**
 * Defines a particular "look" for a player's avatar.
 */
public class Look extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The maximum length of a look's name. */
    public static final int MAX_NAME_LENGTH = 48;

    /** The name of this look (provided by the player). */
    public String name;

    /** The avatar fingerprint associated with this look. */
    public int[] avatar;

    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        return name;
    }
}
