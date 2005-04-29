//
// $Id$

package com.threerings.bang.data;

import com.threerings.parlor.client.GameConfigurator;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.bang.client.BangController;

/**
 * Used to configure a Bang! game.
 */
public class BangConfig extends GameConfig
{
    /** The number of rounds to play. */
    public int rounds = 3;

    /** The amount of with which to start the game. */
    public int startingCash = 300;

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
        return "com.threerings.bang.server.BangManager";
    }
}
