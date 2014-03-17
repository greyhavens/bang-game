//
// $Id$

package com.threerings.bang.avatar.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.avatar.client.AvatarService;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.data.LookConfig;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;

/**
 * Defines the server-side of the {@link AvatarService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from AvatarService.java.")
public interface AvatarProvider extends InvocationProvider
{
    /**
     * Handles a {@link AvatarService#createAvatar} request.
     */
    void createAvatar (PlayerObject caller, Handle arg1, boolean arg2, LookConfig arg3, int arg4, InvocationService.ConfirmListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link AvatarService#selectLook} request.
     */
    void selectLook (PlayerObject caller, Look.Pose arg1, String arg2);
}
