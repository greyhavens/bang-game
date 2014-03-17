//
// $Id$

package com.threerings.bang.saloon.client;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorInfo;

/**
 * Provides saloon-related functionality.
 */
public interface SaloonService extends InvocationService<PlayerObject>
{
    /**
     * Requests that a game be located meeting the specified criterion.
     */
    public void findMatch (Criterion criterion, ResultListener listener);

    /**
     * Requests that we leave our currently pending match.
     */
    public void leaveMatch (int matchOid);

    /**
     * Requests to create a back parlor with the specified configuration.
     */
    public void createParlor (ParlorInfo.Type type, String password, boolean matched,
                              ResultListener rl);

    /**
     * Requests to join the specified back parlor.
     */
    public void joinParlor (Handle creator, String password, ResultListener rl);
}
