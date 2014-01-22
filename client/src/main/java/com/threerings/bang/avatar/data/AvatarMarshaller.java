//
// $Id$

package com.threerings.bang.avatar.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.bang.avatar.client.AvatarService;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;

/**
 * Provides the implementation of the {@link AvatarService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from AvatarService.java.")
public class AvatarMarshaller extends InvocationMarshaller<PlayerObject>
    implements AvatarService
{
    /** The method id used to dispatch {@link #createAvatar} requests. */
    public static final int CREATE_AVATAR = 1;

    // from interface AvatarService
    public void createAvatar (Handle arg1, boolean arg2, LookConfig arg3, int arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(CREATE_AVATAR, new Object[] {
            arg1, Boolean.valueOf(arg2), arg3, Integer.valueOf(arg4), listener5
        });
    }

    /** The method id used to dispatch {@link #selectLook} requests. */
    public static final int SELECT_LOOK = 2;

    // from interface AvatarService
    public void selectLook (Look.Pose arg1, String arg2)
    {
        sendRequest(SELECT_LOOK, new Object[] {
            arg1, arg2
        });
    }
}
