//
// $Id$

package com.threerings.bang.gang.data;

import com.threerings.bang.data.Handle;
import com.threerings.bang.gang.client.GangService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link GangService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class GangMarshaller extends InvocationMarshaller
    implements GangService
{
    /** The method id used to dispatch {@link #inviteMember} requests. */
    public static final int INVITE_MEMBER = 1;

    // from interface GangService
    public void inviteMember (Client arg1, Handle arg2, String arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, INVITE_MEMBER, new Object[] {
            arg2, arg3, listener4
        });
    }
}
