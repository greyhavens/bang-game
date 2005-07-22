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

    /** The number of rounds to play. */
    public int rounds = 3;

    /** The size of each player's team (not including their Big Shot). */
    public int teamSize = 4;

    /** Whether or not fog of war is activated. */
    public boolean fog = false;

    /** A serialized representation of the board to be used. */
    public byte[] boardData;

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
