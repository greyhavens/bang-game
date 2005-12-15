//
// $Id$

package com.threerings.bang.server;

import com.threerings.bang.client.PlayerService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;
import com.threerings.util.Name;

/**
 * Defines the server-side of the {@link PlayerService}.
 */
public interface PlayerProvider extends InvocationProvider
{
    /**
     * Handles a {@link PlayerService#pickFirstBigShot} request.
     */
    public void pickFirstBigShot (ClientObject caller, String arg1, Name arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;
}
