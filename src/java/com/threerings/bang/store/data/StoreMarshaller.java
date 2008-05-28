//
// $Id$

package com.threerings.bang.store.data;

import com.threerings.bang.store.client.StoreService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.net.Transport;

/**
 * Provides the implementation of the {@link StoreService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class StoreMarshaller extends InvocationMarshaller
    implements StoreService
{
    /** The method id used to dispatch {@link #buyGood} requests. */
    public static final int BUY_GOOD = 1;

    // from interface StoreService
    public void buyGood (Client arg1, String arg2, Object[] arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, BUY_GOOD, new Object[] {
            arg2, arg3, listener4
        });
    }
}
