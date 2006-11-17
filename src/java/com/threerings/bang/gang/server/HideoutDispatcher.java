//
// $Id$

package com.threerings.bang.gang.server;

import com.threerings.bang.gang.client.HideoutService;
import com.threerings.bang.gang.data.HideoutMarshaller;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;
import com.threerings.util.Name;

/**
 * Dispatches requests to the {@link HideoutProvider}.
 */
public class HideoutDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public HideoutDispatcher (HideoutProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new HideoutMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case HideoutMarshaller.ADD_TO_COFFERS:
            ((HideoutProvider)provider).addToCoffers(
                source,
                ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue(), (InvocationService.ConfirmListener)args[2]
            );
            return;

        case HideoutMarshaller.FORM_GANG:
            ((HideoutProvider)provider).formGang(
                source,
                (Name)args[0], (String)args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        case HideoutMarshaller.LEAVE_GANG:
            ((HideoutProvider)provider).leaveGang(
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
