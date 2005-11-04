//
// $Id$

package com.threerings.bang.avatar.data;

import com.threerings.bang.avatar.client.AvatarService;
import com.threerings.bang.data.Handle;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

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

    // documentation inherited from interface
    public void createAvatar (Client arg1, Handle arg2, boolean arg3, int arg4, int arg5, String[] arg6, int[] arg7, InvocationService.ConfirmListener arg8)
    {
        InvocationMarshaller.ConfirmMarshaller listener8 = new InvocationMarshaller.ConfirmMarshaller();
        listener8.listener = arg8;
        sendRequest(arg1, CREATE_AVATAR, new Object[] {
            arg2, new Boolean(arg3), new Integer(arg4), new Integer(arg5), arg6, arg7, listener8
        });
    }

    /** The method id used to dispatch {@link #selectLook} requests. */
    public static final int SELECT_LOOK = 2;

    // documentation inherited from interface
    public void selectLook (Client arg1, String arg2)
    {
        sendRequest(arg1, SELECT_LOOK, new Object[] {
            arg2
        });
    }

}
