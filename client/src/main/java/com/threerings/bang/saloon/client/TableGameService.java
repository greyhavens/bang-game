//
// $Id$

package com.threerings.bang.saloon.client;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.saloon.data.ParlorGameConfig;

/**
 * Services available for a table game.
 */
public interface TableGameService extends InvocationService<PlayerObject>
{
    /** Updates this parlor's game configuration. */
    public void updateGameConfig (ParlorGameConfig config);

    /** Requests to start the match-making process with the supplied game configuration. */
    public void startMatchMaking (ParlorGameConfig config,
            byte[] bdata, InvocationService.ConfirmListener listener);

    /** Requests that we join the currently pending match. */
    public void joinMatch ();

    /** Requests that we join the currently pending match at a specific slot. */
    public void joinMatchSlot (int slot);

    /** Requests that we leave the currently pending match. */
    public void leaveMatch ();
}
