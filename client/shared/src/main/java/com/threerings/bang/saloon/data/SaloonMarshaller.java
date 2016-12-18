//
// $Id$

package com.threerings.bang.saloon.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.saloon.client.SaloonService;

/**
 * Provides the implementation of the {@link SaloonService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from SaloonService.java.")
public class SaloonMarshaller extends InvocationMarshaller<PlayerObject>
    implements SaloonService
{
    /** The method id used to dispatch {@link #createParlor} requests. */
    public static final int CREATE_PARLOR = 1;

    // from interface SaloonService
    public void createParlor (ParlorInfo.Type arg1, String arg2, boolean arg3, InvocationService.ResultListener arg4)
    {
        InvocationMarshaller.ResultMarshaller listener4 = new InvocationMarshaller.ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(CREATE_PARLOR, new Object[] {
            arg1, arg2, Boolean.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #findMatch} requests. */
    public static final int FIND_MATCH = 2;

    // from interface SaloonService
    public void findMatch (Criterion arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(FIND_MATCH, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #joinParlor} requests. */
    public static final int JOIN_PARLOR = 3;

    // from interface SaloonService
    public void joinParlor (Handle arg1, String arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(JOIN_PARLOR, new Object[] {
            arg1, arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #leaveMatch} requests. */
    public static final int LEAVE_MATCH = 4;

    // from interface SaloonService
    public void leaveMatch (int arg1)
    {
        sendRequest(LEAVE_MATCH, new Object[] {
            Integer.valueOf(arg1)
        });
    }
}
