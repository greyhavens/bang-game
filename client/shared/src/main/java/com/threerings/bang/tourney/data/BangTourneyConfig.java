//
// $Id$

package com.threerings.bang.tourney.data;

import com.threerings.parlor.tourney.data.TourneyConfig;

/**
 * Tourney configuration data.
 */
public class BangTourneyConfig extends TourneyConfig
{
    /** The tourney description. */
    public String desc;

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        com.threerings.io.GenStreamUtil.readField(com.threerings.parlor.tourney.data.TourneyConfig.class, "tourneyId", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.parlor.tourney.data.TourneyConfig.class, "prize", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.parlor.tourney.data.TourneyConfig.class, "entryFee", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.parlor.tourney.data.TourneyConfig.class, "creator", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.parlor.tourney.data.TourneyConfig.class, "minPlayers", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.parlor.tourney.data.TourneyConfig.class, "startsIn", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.parlor.tourney.data.TourneyConfig.class, "logic", this, ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.tourney.data.BangTourneyConfig.class, "desc", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        com.threerings.io.GenStreamUtil.writeField(com.threerings.parlor.tourney.data.TourneyConfig.class, "tourneyId", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.parlor.tourney.data.TourneyConfig.class, "prize", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.parlor.tourney.data.TourneyConfig.class, "entryFee", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.parlor.tourney.data.TourneyConfig.class, "creator", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.parlor.tourney.data.TourneyConfig.class, "minPlayers", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.parlor.tourney.data.TourneyConfig.class, "startsIn", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.parlor.tourney.data.TourneyConfig.class, "logic", this, out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.tourney.data.BangTourneyConfig.class, "desc", this, out);
    }
}
