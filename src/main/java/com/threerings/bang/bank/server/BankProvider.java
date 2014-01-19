//
// $Id$

package com.threerings.bang.bank.server;

import com.threerings.bang.bank.client.BankService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link BankService}.
 */
public interface BankProvider extends InvocationProvider
{
    /**
     * Handles a {@link BankService#cancelOffer} request.
     */
    void cancelOffer (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link BankService#getMyOffers} request.
     */
    void getMyOffers (ClientObject caller, BankService.OfferListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link BankService#postOffer} request.
     */
    void postOffer (ClientObject caller, int arg1, int arg2, boolean arg3, boolean arg4, InvocationService.ResultListener arg5)
        throws InvocationException;
}
