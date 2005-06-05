//
// $Id$

package com.threerings.bang.ranch.data;

import com.threerings.bang.ranch.client.RanchService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link RanchService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class RanchMarshaller extends InvocationMarshaller
    implements RanchService
{
    /** The method id used to dispatch {@link #recruitBigShot} requests. */
    public static final int RECRUIT_BIG_SHOT = 1;

    // documentation inherited from interface
    public void recruitBigShot (Client arg1, String arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, RECRUIT_BIG_SHOT, new Object[] {
            arg2, listener3
        });
    }

}
