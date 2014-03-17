//
// $Id$

package com.threerings.bang.store.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.store.client.StoreService;

/**
 * Provides the implementation of the {@link StoreService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from StoreService.java.")
public class StoreMarshaller extends InvocationMarshaller<PlayerObject>
    implements StoreService
{
    /** The method id used to dispatch {@link #buyGood} requests. */
    public static final int BUY_GOOD = 1;

    // from interface StoreService
    public void buyGood (String arg1, Object[] arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(BUY_GOOD, new Object[] {
            arg1, arg2, listener3
        });
    }
}
