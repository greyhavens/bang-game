//
// $Id$

package com.threerings.bang.server;

import com.threerings.bang.client.PlayerService;
import com.threerings.bang.data.PlayerMarshaller;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;
import com.threerings.util.Name;

/**
 * Dispatches requests to the {@link PlayerProvider}.
 */
public class PlayerDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public PlayerDispatcher (PlayerProvider provider)
    {
        this.provider = provider;
    }

    // documentation inherited
    public InvocationMarshaller createMarshaller ()
    {
        return new PlayerMarshaller();
    }

    // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case PlayerMarshaller.PICK_FIRST_BIG_SHOT:
            ((PlayerProvider)provider).pickFirstBigShot(
                source,
                (String)args[0], (Name)args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
        }
    }
}
