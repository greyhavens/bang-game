//
// $Id$

package com.threerings.bang.admin.server;

import javax.annotation.Generated;

import com.threerings.presents.server.InvocationProvider;

import com.threerings.bang.admin.client.BangAdminService;
import com.threerings.bang.data.PlayerObject;

/**
 * Defines the server-side of the {@link BangAdminService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from BangAdminService.java.")
public interface BangAdminProvider extends InvocationProvider
{
    /**
     * Handles a {@link BangAdminService#scheduleReboot} request.
     */
    void scheduleReboot (PlayerObject caller, int arg1);
}
