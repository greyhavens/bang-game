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

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.ParlorGameConfig.class, "rounds", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.ParlorGameConfig.class, "slots", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.ParlorGameConfig.class, "teamSize", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.ParlorGameConfig.class, "duration", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.ParlorGameConfig.class, "speed", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.ParlorGameConfig.class, "scenarios", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.saloon.data.ParlorGameConfig.class, "mode", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.ParlorGameConfig.class, "rounds", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.ParlorGameConfig.class, "slots", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.ParlorGameConfig.class, "teamSize", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.ParlorGameConfig.class, "duration", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.ParlorGameConfig.class, "speed", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.ParlorGameConfig.class, "scenarios", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.saloon.data.ParlorGameConfig.class, "mode", this, out);
    }
}
