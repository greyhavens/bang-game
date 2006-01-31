//
// $Id$

package com.threerings.bang.saloon.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.saloon.data.Criterion;

/**
 * Provides saloon-related functionality.
 */
public interface SaloonService extends InvocationService
{
    /**
     * Requests that a game be located meeting the specified criterion.
     */
    public void findMatch (
        Client client, Criterion criterion, ResultListener listener);

    /**
     * Requests that we leave our currently pending match.
     */
    public void leaveMatch (Client client, int matchOid);
}
