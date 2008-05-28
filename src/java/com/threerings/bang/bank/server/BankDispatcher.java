//
// $Id$

package com.threerings.bang.bank.server;

import com.threerings.bang.bank.client.BankService;
import com.threerings.bang.bank.data.BankMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link BankProvider}.
 */
public class BankDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public BankDispatcher (BankProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public InvocationMarshaller createMarshaller ()
    {
        return new BankMarshaller();
    }

    @SuppressWarnings("unchecked")
    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case BankMarshaller.CANCEL_OFFER:
            ((BankProvider)provider).cancelOffer(
                source,
                ((Integer)args[0]).intValue(), (InvocationService.ConfirmListener)args[1]
            );
            return;

        case BankMarshaller.GET_MY_OFFERS:
            ((BankProvider)provider).getMyOffers(
                source,
                (BankService.OfferListener)args[0]
            );
            return;

        case BankMarshaller.POST_OFFER:
            ((BankProvider)provider).postOffer(
                source,
                ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue(), ((Boolean)args[2]).booleanValue(), ((Boolean)args[3]).booleanValue(), (InvocationService.ResultListener)args[4]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
