//
// $Id$

package com.threerings.bang.gang.server;

import com.threerings.bang.data.Handle;
import com.threerings.bang.gang.data.GangMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link GangProvider}.
 */
public class GangDispatcher extends InvocationDispatcher<GangMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public GangDispatcher (GangProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public GangMarshaller createMarshaller ()
    {
        return new GangMarshaller();
    }

    @SuppressWarnings("unchecked")
    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case GangMarshaller.GET_GANG_INFO:
            ((GangProvider)provider).getGangInfo(
                source,
                (Handle)args[0], (InvocationService.ResultListener)args[1]
            );
            return;

        case GangMarshaller.INVITE_MEMBER:
            ((GangProvider)provider).inviteMember(
                source,
                (Handle)args[0], (String)args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
