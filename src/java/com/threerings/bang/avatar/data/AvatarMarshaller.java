//
// $Id$

package com.threerings.bang.avatar.data;

import com.threerings.bang.avatar.client.AvatarService;
import com.threerings.presents.client.Client;
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
    /** The method id used to dispatch {@link #selectLook} requests. */
    public static final int SELECT_LOOK = 1;

    // documentation inherited from interface
    public void selectLook (Client arg1, String arg2)
    {
        sendRequest(arg1, SELECT_LOOK, new Object[] {
            arg2
        });
    }

}
