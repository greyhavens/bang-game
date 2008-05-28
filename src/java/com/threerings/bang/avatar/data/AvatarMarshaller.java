//
// $Id$

package com.threerings.bang.avatar.data;

import com.threerings.bang.avatar.client.AvatarService;
import com.threerings.bang.data.Handle;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.net.Transport;

/**
 * Provides the implementation of the {@link AvatarService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class AvatarMarshaller extends InvocationMarshaller
    implements AvatarService
{
    /** The method id used to dispatch {@link #createAvatar} requests. */
    public static final int CREATE_AVATAR = 1;

    // from interface AvatarService
    public void createAvatar (Client arg1, Handle arg2, boolean arg3, LookConfig arg4, int arg5, InvocationService.ConfirmListener arg6)
    {
        InvocationMarshaller.ConfirmMarshaller listener6 = new InvocationMarshaller.ConfirmMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, CREATE_AVATAR, new Object[] {
            arg2, Boolean.valueOf(arg3), arg4, Integer.valueOf(arg5), listener6
        });
    }

    /** The method id used to dispatch {@link #selectLook} requests. */
    public static final int SELECT_LOOK = 2;

    // from interface AvatarService
    public void selectLook (Client arg1, Look.Pose arg2, String arg3)
    {
        sendRequest(arg1, SELECT_LOOK, new Object[] {
            arg2, arg3
        });
    }
}
