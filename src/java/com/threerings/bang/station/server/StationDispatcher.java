//
// $Id$

package com.threerings.bang.station.server;

import com.threerings.bang.station.data.StationMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link StationProvider}.
 */
public class StationDispatcher extends InvocationDispatcher<StationMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public StationDispatcher (StationProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public StationMarshaller createMarshaller ()
    {
        return new StationMarshaller();
    }

    @SuppressWarnings("unchecked")
    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case StationMarshaller.ACTIVATE_TICKET:
            ((StationProvider)provider).activateTicket(
                source,
                (InvocationService.ConfirmListener)args[0]
            );
            return;

        case StationMarshaller.BUY_TICKET:
            ((StationProvider)provider).buyTicket(
                source,
                (InvocationService.ConfirmListener)args[0]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
