//
// $Id$

package com.threerings.bang.server;

import com.threerings.bang.data.BangPeerMarshaller;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.Item;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link BangPeerProvider}.
 */
public class BangPeerDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public BangPeerDispatcher (BangPeerProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public InvocationMarshaller createMarshaller ()
    {
        return new BangPeerMarshaller();
    }

    @SuppressWarnings("unchecked")
    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case BangPeerMarshaller.DELIVER_GANG_INVITE:
            ((BangPeerProvider)provider).deliverGangInvite(
                source,
                (Handle)args[0], (Handle)args[1], ((Integer)args[2]).intValue(), (Handle)args[3], (String)args[4]
            );
            return;

        case BangPeerMarshaller.DELIVER_ITEM:
            ((BangPeerProvider)provider).deliverItem(
                source,
                (Item)args[0], (String)args[1]
            );
            return;

        case BangPeerMarshaller.DELIVER_PARDNER_INVITE:
            ((BangPeerProvider)provider).deliverPardnerInvite(
                source,
                (Handle)args[0], (Handle)args[1], (String)args[2]
            );
            return;

        case BangPeerMarshaller.DELIVER_PARDNER_INVITE_RESPONSE:
            ((BangPeerProvider)provider).deliverPardnerInviteResponse(
                source,
                (Handle)args[0], (Handle)args[1], ((Boolean)args[2]).booleanValue(), ((Boolean)args[3]).booleanValue()
            );
            return;

        case BangPeerMarshaller.DELIVER_PARDNER_REMOVAL:
            ((BangPeerProvider)provider).deliverPardnerRemoval(
                source,
                (Handle)args[0], (Handle)args[1]
            );
            return;

        case BangPeerMarshaller.GET_GANG_OID:
            ((BangPeerProvider)provider).getGangOid(
                source,
                ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
