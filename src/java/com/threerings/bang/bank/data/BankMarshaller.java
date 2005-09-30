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
    /** The method id used to dispatch {@link #postOffer} requests. */
    public static final int POST_OFFER = 1;

    // documentation inherited from interface
    public void postOffer (Client arg1, int arg2, int arg3, boolean arg4, boolean arg5, InvocationService.ConfirmListener arg6)
    {
        InvocationMarshaller.ConfirmMarshaller listener6 = new InvocationMarshaller.ConfirmMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, POST_OFFER, new Object[] {
            new Integer(arg2), new Integer(arg3), new Boolean(arg4), new Boolean(arg5), listener6
        });
    }

}
