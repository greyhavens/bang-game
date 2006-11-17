//
// $Id$

package com.threerings.bang.gang.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.Handle;

/**
 * Provides gang-related functionality.
 */
public interface GangService extends InvocationService
{
    /**
     * Invite the specified user to be a member of our gang.
     */
    public void inviteMember (
        Client client, Handle handle, String message,
        ConfirmListener listener);
}
