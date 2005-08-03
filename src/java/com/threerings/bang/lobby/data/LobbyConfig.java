//
// $Id$

package com.threerings.bang.lobby.data;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.bang.lobby.client.LobbyController;

/**
 * Defines the configuration for the Bang! lobby.
 */
public class LobbyConfig extends PlaceConfig
{
    /** The town in which this lobby resides. */
    public String townId;

    // documentation inherited
    public Class getControllerClass ()
    {
        return LobbyController.class;
    }

    // documentation inherited
    public String getManagerClassName ()
    {
        return "com.threerings.bang.lobby.server.LobbyManager";
    }
}
