//
// $Id$

package com.threerings.bang.lobby.data;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.bang.lobby.client.LobbyController;

/**
 * Defines the configuration for the Bang! lobby.
 */
public class LobbyConfig extends PlaceConfig
{
    // documentation inherited
    public PlaceController createController ()
    {
        return new LobbyController();
    }

    // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.lobby.server.LobbyManager";
    }
}
