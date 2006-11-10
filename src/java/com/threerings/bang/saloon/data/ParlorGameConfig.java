//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.bang.game.data.BangConfig;

/**
 * Contains information about a Bang game being configured in a Back Parlor.
 */
public class ParlorGameConfig extends SimpleStreamableObject
{
    /** The number of rounds for the current game. */
    public int rounds;

    /** The number of players in the current game. */
    public int players;

    /** The number of tin cans in the current game. */
    public int tinCans;

    /** The team size of the current game. */
    public int teamSize;

    /** The duration adjustment for the game. */
    public BangConfig.Duration duration;

    /** The speed adjustment for the game. */
    public BangConfig.Speed speed;

    /** The scenarios allowed for the current game. */
    public String[] scenarios;
}
