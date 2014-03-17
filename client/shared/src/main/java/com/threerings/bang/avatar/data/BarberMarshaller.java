//
// $Id$

package com.threerings.bang.avatar.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.avatar.client.BarberService;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;

/**
 * Provides the implementation of the {@link BarberService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from BarberService.java.")
public class BarberMarshaller extends InvocationMarshaller<PlayerObject>
    implements BarberService
{
    /** The method id used to dispatch {@link #changeHandle} requests. */
    public static final int CHANGE_HANDLE = 1;

    // from interface BarberService
    public void changeHandle (Handle arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(CHANGE_HANDLE, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #configureLook} requests. */
    public static final int CONFIGURE_LOOK = 2;

    // from interface BarberService
    public void configureLook (String arg1, int[] arg2)
    {
        sendRequest(CONFIGURE_LOOK, new Object[] {
            arg1, arg2
        });
    }

    /** The method id used to dispatch {@link #purchaseLook} requests. */
    public static final int PURCHASE_LOOK = 3;

    // from interface BarberService
    public void purchaseLook (LookConfig arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(PURCHASE_LOOK, new Object[] {
            arg1, listener2
        });
    }
}
