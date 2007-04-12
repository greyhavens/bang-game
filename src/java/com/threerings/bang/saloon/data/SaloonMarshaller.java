//
// $Id$

package com.threerings.bang.saloon.data;

import com.threerings.bang.data.Handle;
import com.threerings.bang.saloon.client.SaloonService;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link SaloonService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class SaloonMarshaller extends InvocationMarshaller
    implements SaloonService
{
    /** The method id used to dispatch {@link #createParlor} requests. */
    public static final int CREATE_PARLOR = 1;

    // from interface SaloonService
    public void createParlor (Client arg1, ParlorInfo.Type arg2, String arg3, boolean arg4, InvocationService.ResultListener arg5)
    {
        InvocationMarshaller.ResultMarshaller listener5 = new InvocationMarshaller.ResultMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, CREATE_PARLOR, new Object[] {
            arg2, arg3, Boolean.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #findMatch} requests. */
    public static final int FIND_MATCH = 2;

    // from interface SaloonService
    public void findMatch (Client arg1, Criterion arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, FIND_MATCH, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #joinParlor} requests. */
    public static final int JOIN_PARLOR = 3;

    // from interface SaloonService
    public void joinParlor (Client arg1, Handle arg2, String arg3, InvocationService.ResultListener arg4)
    {
        InvocationMarshaller.ResultMarshaller listener4 = new InvocationMarshaller.ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, JOIN_PARLOR, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #leaveMatch} requests. */
    public static final int LEAVE_MATCH = 4;

    // from interface SaloonService
    public void leaveMatch (Client arg1, int arg2)
    {
        sendRequest(arg1, LEAVE_MATCH, new Object[] {
            Integer.valueOf(arg2)
        });
    }
}
