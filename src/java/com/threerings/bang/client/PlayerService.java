//
// $Id$

package com.threerings.bang.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.util.Name;

/**
 * A general purpose bootstrap invocation service.
 */
public interface PlayerService extends InvocationService
{
    /**
     * Issues a request to create this player's (free) first Big Shot.
     */
    public void pickFirstBigShot (
        Client client, String type, Name name, ConfirmListener cl);
    
    /**
     * Invite the specified user to be our pardner.
     */
    public void invitePardner (Client client, Name handle,
        ConfirmListener listener);

    /**
     * Respond to another cowpoke's invitation to be pardners.
     */
    public void respondToPardnerInvite (Client client, Name inviter,
        boolean resp, ConfirmListener listener);
    
    /**
     * Remove one of our pardners from our pardner list.
     */
    public void removePardner (Client client, Name pardner,
        ConfirmListener listener);
}
