//
// $Id$

package com.threerings.bang.avatar.server;

import com.threerings.bang.avatar.client.BarberService;
import com.threerings.bang.avatar.data.BarberMarshaller;
import com.threerings.bang.avatar.data.LookConfig;
import com.threerings.bang.data.Handle;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link BarberProvider}.
 */
public class BarberDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public BarberDispatcher (BarberProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new BarberMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case BarberMarshaller.CHANGE_HANDLE:
            ((BarberProvider)provider).changeHandle(
                source,
                (Handle)args[0], (InvocationService.ConfirmListener)args[1]
            );
            return;

        case BarberMarshaller.CONFIGURE_LOOK:
            ((BarberProvider)provider).configureLook(
                source,
                (String)args[0], (int[])args[1]
            );
            return;

        case BarberMarshaller.PURCHASE_LOOK:
            ((BarberProvider)provider).purchaseLook(
                source,
                (LookConfig)args[0], (InvocationService.ConfirmListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
