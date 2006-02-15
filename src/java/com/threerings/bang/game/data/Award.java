//
// $Id$

package com.threerings.bang.game.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.data.Badge;

/**
 * Used to record and report awards at the end of a game.
 */
public class Award extends SimpleStreamableObject
{
    /** The amount of cash "taken home" by this player. */
    public int cashEarned;

    /** The badge earned by this player if any. */
    public Badge badge;

    /** Default constructor used during unserialization. */
    public Award ()
    {
    }
}
