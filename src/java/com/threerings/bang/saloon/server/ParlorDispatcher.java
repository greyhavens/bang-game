//
// $Id$

package com.threerings.bang.saloon.server;

import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.bang.saloon.data.ParlorMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link ParlorProvider}.
 */
public class ParlorDispatcher extends InvocationDispatcher<ParlorMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public ParlorDispatcher (ParlorProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public ParlorMarshaller createMarshaller ()
    {
        return new ParlorMarshaller();
    }

    @SuppressWarnings("unchecked")
    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case ParlorMarshaller.BOOT_PLAYER:
            ((ParlorProvider)provider).bootPlayer(
                source,
                ((Integer)args[0]).intValue()
            );
            return;

        case ParlorMarshaller.FIND_SALOON_MATCH:
            ((ParlorProvider)provider).findSaloonMatch(
                source,
                (Criterion)args[0], (InvocationService.ResultListener)args[1]
            );
            return;

        case ParlorMarshaller.LEAVE_SALOON_MATCH:
            ((ParlorProvider)provider).leaveSaloonMatch(
                source,
                ((Integer)args[0]).intValue()
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
