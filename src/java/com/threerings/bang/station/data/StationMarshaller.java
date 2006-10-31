//
// $Id$

package com.threerings.bang.station.data;

import com.threerings.bang.station.client.StationService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link StationService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class StationMarshaller extends InvocationMarshaller
    implements StationService
{
    /** The method id used to dispatch {@link #buyTicket} requests. */
    public static final int BUY_TICKET = 1;

    // from interface StationService
    public void buyTicket (Client arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, BUY_TICKET, new Object[] {
            listener2
        });
    }
}
