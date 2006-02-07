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
    /** The method id used to dispatch {@link #invitePardner} requests. */
    public static final int INVITE_PARDNER = 1;

    // documentation inherited from interface
    public void invitePardner (Client arg1, Name arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, INVITE_PARDNER, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #pickFirstBigShot} requests. */
    public static final int PICK_FIRST_BIG_SHOT = 2;

    // documentation inherited from interface
    public void pickFirstBigShot (Client arg1, String arg2, Name arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, PICK_FIRST_BIG_SHOT, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #removePardner} requests. */
    public static final int REMOVE_PARDNER = 3;

    // documentation inherited from interface
    public void removePardner (Client arg1, Name arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, REMOVE_PARDNER, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #respondToPardnerInvite} requests. */
    public static final int RESPOND_TO_PARDNER_INVITE = 4;

    // documentation inherited from interface
    public void respondToPardnerInvite (Client arg1, Name arg2, boolean arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, RESPOND_TO_PARDNER_INVITE, new Object[] {
            arg2, new Boolean(arg3), listener4
        });
    }

}
