//
// $Id$

package com.threerings.bang.saloon.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.saloon.data.ParlorGameConfig;
import com.threerings.bang.saloon.data.ParlorInfo;

/**
 * Services available while in a Back Parlor.
 */
public interface ParlorService extends InvocationService
{
    /** Updates this parlor's parlor configuration. */
    public void updateParlorConfig (
        Client client, ParlorInfo info, boolean creatorStart);

    /** Updates this parlor's password. */
    public void updateParlorPassword (Client client, String password);

    /** Updates this parlor's game configuration. */
    public void updateGameConfig (Client client, ParlorGameConfig config);

    /** Requests to start the match-making process with the supplied game
     * configuration. */
    public void startMatchMaking (Client client, ParlorGameConfig config,
                                  byte[] bdata, InvocationListener listener);

    /** Requests that we join the currently pending match. */
    public void joinMatch (Client client);

    /** Requests that we leave the currently pending match. */
    public void leaveMatch (Client client);
}
