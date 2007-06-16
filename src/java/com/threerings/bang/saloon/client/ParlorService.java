//
// $Id$

package com.threerings.bang.saloon.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorGameConfig;
import com.threerings.bang.saloon.data.ParlorInfo;

/**
 * Services available while in a Back Parlor.
 */
public interface ParlorService extends InvocationService
{
    /** Updates this parlor's parlor configuration. */
    public void updateParlorConfig (
        Client client, ParlorInfo info, boolean creatorStart);

    /** Updates this parlor's password. */
    public void updateParlorPassword (Client client, String password);

    /** Requests that a game be located meeting the speified criterion. */
    public void findSaloonMatch (Client client, Criterion criterion, ResultListener listener);

    /** Requests that we leave our currently pending match. */
    public void leaveSaloonMatch (Client client, int matchOid);

    /** Requests that a player be booted from a back parlor. */
    public void bootPlayer (Client client, int bodyOid);
}
