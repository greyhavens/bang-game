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
    /** Indicates the game mode. */
    public static enum Mode {
        NORMAL, TEAM_2V2
    };

    /** Indicates the state of a player slot. */
    public static enum Slot {
        HUMAN, TINCAN, NONE
    };

    /** The number of rounds for the current game. */
    public int rounds;

    /** The configuration of game slots. */
    public Slot[] slots = new Slot[4];

    /** The team size of the current game. */
    public int teamSize;

    /** The duration adjustment for the game. */
    public BangConfig.Duration duration;

    /** The speed adjustment for the game. */
    public BangConfig.Speed speed;

    /** The scenarios allowed for the current game. */
    public String[] scenarios;

    /** The configured mode. */
    public Mode mode;

    public int getCount (Slot type)
    {
        int count = 0;
        for (Slot slot : slots) {
            if (slot == type) {
                count++;
            }
        }
        return count;
    }
}
