//
// $Id$

package com.threerings.bang.bank.server;

import com.threerings.bang.bank.client.BankService;
import com.threerings.presents.client.Client;
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
     * Handles a {@link BankService#buyCoins} request.
     */
    public void buyCoins (ClientObject caller, int arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link BankService#postBuyOffer} request.
     */
    public void postBuyOffer (ClientObject caller, int arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link BankService#postSellOffer} request.
     */
    public void postSellOffer (ClientObject caller, int arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link BankService#sellCoins} request.
     */
    public void sellCoins (ClientObject caller, int arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;
}
