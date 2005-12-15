//
// $Id$

package com.threerings.bang.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * A general purpose bootstrap invocation service.
 */
public interface PlayerService extends InvocationService
{
    /**
     * Issues a request to create this player's (free) first Big Shot.
     */
    public void pickFirstBigShot (
        Client client, String type, ConfirmListener cl);
}
