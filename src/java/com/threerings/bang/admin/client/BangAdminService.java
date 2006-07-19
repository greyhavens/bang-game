//
// $Id$

package com.threerings.bang.admin.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Provides admin functionality for Bang!.
 */
public interface BangAdminService extends InvocationService
{
    /**
     * Requests to schedule a reboot for the specified number of minutes in the
     * future.
     */
    public void scheduleReboot (Client client, int minutes);
}
