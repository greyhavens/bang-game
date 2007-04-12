//
// $Id$

package com.threerings.bang.saloon.server;

import com.threerings.bang.data.Handle;
import com.threerings.bang.saloon.client.SaloonService;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.bang.saloon.data.SaloonMarshaller;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link SaloonProvider}.
 */
public class SaloonDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public SaloonDispatcher (SaloonProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new SaloonMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case SaloonMarshaller.CREATE_PARLOR:
            ((SaloonProvider)provider).createParlor(
                source,
                (ParlorInfo.Type)args[0], (String)args[1], ((Boolean)args[2]).booleanValue(), (InvocationService.ResultListener)args[3]
            );
            return;

        case SaloonMarshaller.FIND_MATCH:
            ((SaloonProvider)provider).findMatch(
                source,
                (Criterion)args[0], (InvocationService.ResultListener)args[1]
            );
            return;

        case SaloonMarshaller.JOIN_PARLOR:
            ((SaloonProvider)provider).joinParlor(
                source,
                (Handle)args[0], (String)args[1], (InvocationService.ResultListener)args[2]
            );
            return;

        case SaloonMarshaller.LEAVE_MATCH:
            ((SaloonProvider)provider).leaveMatch(
                source,
                ((Integer)args[0]).intValue()
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
