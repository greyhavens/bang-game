//
// $Id$

package com.threerings.bang.admin.client;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.PlayerObject;

/**
 * Provides admin functionality for Bang!.
 */
public interface BangAdminService extends InvocationService<PlayerObject>
{
    /**
     * Requests to schedule a reboot for the specified number of minutes in the
     * future.
     */
    public void scheduleReboot (int minutes);
}
