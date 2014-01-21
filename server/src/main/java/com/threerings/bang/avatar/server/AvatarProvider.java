//
// $Id$

package com.threerings.bang.avatar.server;

import com.threerings.bang.avatar.client.AvatarService;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.data.LookConfig;
import com.threerings.bang.data.Handle;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link AvatarService}.
 */
public interface AvatarProvider extends InvocationProvider
{
    /**
     * Handles a {@link AvatarService#createAvatar} request.
     */
    void createAvatar (ClientObject caller, Handle arg1, boolean arg2, LookConfig arg3, int arg4, InvocationService.ConfirmListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link AvatarService#selectLook} request.
     */
    void selectLook (ClientObject caller, Look.Pose arg1, String arg2);
}
