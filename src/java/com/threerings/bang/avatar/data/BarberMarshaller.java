//
// $Id$

package com.threerings.bang.avatar.data;

import com.threerings.bang.avatar.client.BarberService;
import com.threerings.bang.avatar.data.Look;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link BarberService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class BarberMarshaller extends InvocationMarshaller
    implements BarberService
{
    /** The method id used to dispatch {@link #configureLook} requests. */
    public static final int CONFIGURE_LOOK = 1;

    // documentation inherited from interface
    public void configureLook (Client arg1, String arg2, int[] arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, CONFIGURE_LOOK, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #purchaseLook} requests. */
    public static final int PURCHASE_LOOK = 2;

    // documentation inherited from interface
    public void purchaseLook (Client arg1, Look arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, PURCHASE_LOOK, new Object[] {
            arg2, listener3
        });
    }

}
