//
// $Id$

package com.threerings.bang.gang.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.gang.client.GangService;

/**
 * Provides the implementation of the {@link GangService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from GangService.java.")
public class GangMarshaller extends InvocationMarshaller<PlayerObject>
    implements GangService
{
    /** The method id used to dispatch {@link #getGangInfo} requests. */
    public static final int GET_GANG_INFO = 1;

    // from interface GangService
    public void getGangInfo (Handle arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_GANG_INFO, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #inviteMember} requests. */
    public static final int INVITE_MEMBER = 2;

    // from interface GangService
    public void inviteMember (Handle arg1, String arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(INVITE_MEMBER, new Object[] {
            arg1, arg2, listener3
        });
    }
}
