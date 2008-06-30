//
// $Id$

package com.threerings.bang.store.server;

import com.threerings.bang.store.data.StoreMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link StoreProvider}.
 */
public class StoreDispatcher extends InvocationDispatcher<StoreMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public StoreDispatcher (StoreProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public StoreMarshaller createMarshaller ()
    {
        return new StoreMarshaller();
    }

    @SuppressWarnings("unchecked")
    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case StoreMarshaller.BUY_GOOD:
            ((StoreProvider)provider).buyGood(
                source,
                (String)args[0], (Object[])args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
