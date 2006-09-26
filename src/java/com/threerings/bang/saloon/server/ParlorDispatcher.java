//
// $Id$

package com.threerings.bang.saloon.server;

import com.threerings.bang.saloon.client.ParlorService;
import com.threerings.bang.saloon.data.ParlorGameConfig;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.bang.saloon.data.ParlorMarshaller;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link ParlorProvider}.
 */
public class ParlorDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public ParlorDispatcher (ParlorProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new ParlorMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case ParlorMarshaller.JOIN_MATCH:
            ((ParlorProvider)provider).joinMatch(
                source                
            );
            return;

        case ParlorMarshaller.LEAVE_MATCH:
            ((ParlorProvider)provider).leaveMatch(
                source                
            );
            return;

        case ParlorMarshaller.START_MATCH_MAKING:
            ((ParlorProvider)provider).startMatchMaking(
                source,
                (ParlorGameConfig)args[0], (byte[])args[1], (InvocationService.InvocationListener)args[2]
            );
            return;

        case ParlorMarshaller.UPDATE_GAME_CONFIG:
            ((ParlorProvider)provider).updateGameConfig(
                source,
                (ParlorGameConfig)args[0]
            );
            return;

        case ParlorMarshaller.UPDATE_PARLOR_CONFIG:
            ((ParlorProvider)provider).updateParlorConfig(
                source,
                (ParlorInfo)args[0], ((Boolean)args[1]).booleanValue()
            );
            return;

        case ParlorMarshaller.UPDATE_PARLOR_PASSWORD:
            ((ParlorProvider)provider).updateParlorPassword(
                source,
                (String)args[0]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
