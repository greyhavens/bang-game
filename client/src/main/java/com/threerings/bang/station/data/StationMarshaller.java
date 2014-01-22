//
// $Id$

package com.threerings.bang.station.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.station.client.StationService;

/**
 * Provides the implementation of the {@link StationService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from StationService.java.")
public class StationMarshaller extends InvocationMarshaller<PlayerObject>
    implements StationService
{
    /** The method id used to dispatch {@link #activateTicket} requests. */
    public static final int ACTIVATE_TICKET = 1;

    // from interface StationService
    public void activateTicket (InvocationService.ConfirmListener arg1)
    {
        InvocationMarshaller.ConfirmMarshaller listener1 = new InvocationMarshaller.ConfirmMarshaller();
        listener1.listener = arg1;
        sendRequest(ACTIVATE_TICKET, new Object[] {
            listener1
        });
    }

    /** The method id used to dispatch {@link #buyTicket} requests. */
    public static final int BUY_TICKET = 2;

    // from interface StationService
    public void buyTicket (InvocationService.ConfirmListener arg1)
    {
        InvocationMarshaller.ConfirmMarshaller listener1 = new InvocationMarshaller.ConfirmMarshaller();
        listener1.listener = arg1;
        sendRequest(BUY_TICKET, new Object[] {
            listener1
        });
    }
}
