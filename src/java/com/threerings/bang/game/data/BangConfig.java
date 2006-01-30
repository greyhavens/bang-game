//
// $Id$

package com.threerings.bang.game.data;

import com.threerings.parlor.game.client.GameConfigurator;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.bang.game.client.BangController;

/**
 * Used to configure a Bang! game.
 */
public class BangConfig extends GameConfig
{
    /** The number of people playing the game. */
    public int seats = 2;

    /** The size of each player's team (not including their Big Shot). */
    public int teamSize = 4;

    /** The desired scenarios for each round (implies the number of rounds). */
    public String[] scenarios;

    /** If true, the scenario will be interpreted as a tutorial identifier and
     * a tutorial game will be created. */
    public boolean tutorial = false;

    /** Specifies the exact name of the board to be used instead of choosing
     * randomly. This is used when testing. */
    public String board;

    /** Used when testing with a specific board. */
    public byte[] bdata;

    /** Whether or not fog of war is activated. */
    public boolean fog = false;

    /** Returns the desired number of rounds. */
    public int getRounds ()
    {
        return scenarios.length;
    }

    /** Helper for {@link #toString}. */
    public String bdataToString ()
    {
        return (bdata == null) ? "<none>" : (bdata.length + " bytes");
    }

    @Override // documentation inherited
    public String getBundleName ()
    {
        return "bang";
    }

    @Override // documentation inherited
    public GameConfigurator createConfigurator ()
    {
        return null;
    }

    @Override // documentation inherited
    public Class getControllerClass ()
    {
        return BangController.class;
    }

    @Override // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.game.server.BangManager";
    }
}
