//
// $Id$

package com.threerings.bang.bank.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.bank.client.BankService;
import com.threerings.bang.data.PlayerObject;

/**
 * Defines the server-side of the {@link BankService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from BankService.java.")
public interface BankProvider extends InvocationProvider
{
    /**
     * Handles a {@link BankService#cancelOffer} request.
     */
    void cancelOffer (PlayerObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link BankService#getMyOffers} request.
     */
    void getMyOffers (PlayerObject caller, BankService.OfferListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link BankService#postOffer} request.
     */
    void postOffer (PlayerObject caller, int arg1, int arg2, boolean arg3, boolean arg4, InvocationService.ResultListener arg5)
        throws InvocationException;
}
