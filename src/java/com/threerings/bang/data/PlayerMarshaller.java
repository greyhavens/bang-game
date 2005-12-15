//
// $Id$

package com.threerings.bang.data;

import com.threerings.bang.client.PlayerService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;
import com.threerings.util.Name;

/**
 * Provides the implementation of the {@link PlayerService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PlayerMarshaller extends InvocationMarshaller
    implements PlayerService
{
    /** The method id used to dispatch {@link #pickFirstBigShot} requests. */
    public static final int PICK_FIRST_BIG_SHOT = 1;

    // documentation inherited from interface
    public void pickFirstBigShot (Client arg1, String arg2, Name arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, PICK_FIRST_BIG_SHOT, new Object[] {
            arg2, arg3, listener4
        });
    }

}
