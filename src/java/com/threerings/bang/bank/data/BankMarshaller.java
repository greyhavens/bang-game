//
// $Id$

package com.threerings.bang.bank.data;

import com.threerings.bang.bank.client.BankService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link BankService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class BankMarshaller extends InvocationMarshaller
    implements BankService
{
    /** The method id used to dispatch {@link #buyCoins} requests. */
    public static final int BUY_COINS = 1;

    // documentation inherited from interface
    public void buyCoins (Client arg1, int arg2, int arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, BUY_COINS, new Object[] {
            new Integer(arg2), new Integer(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #postBuyOffer} requests. */
    public static final int POST_BUY_OFFER = 2;

    // documentation inherited from interface
    public void postBuyOffer (Client arg1, int arg2, int arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, POST_BUY_OFFER, new Object[] {
            new Integer(arg2), new Integer(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #postSellOffer} requests. */
    public static final int POST_SELL_OFFER = 3;

    // documentation inherited from interface
    public void postSellOffer (Client arg1, int arg2, int arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, POST_SELL_OFFER, new Object[] {
            new Integer(arg2), new Integer(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #sellCoins} requests. */
    public static final int SELL_COINS = 4;

    // documentation inherited from interface
    public void sellCoins (Client arg1, int arg2, int arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, SELL_COINS, new Object[] {
            new Integer(arg2), new Integer(arg3), listener4
        });
    }

}
