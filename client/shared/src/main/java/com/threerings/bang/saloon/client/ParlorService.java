//
// $Id$

package com.threerings.bang.saloon.client;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorInfo;

/**
 * Services available while in a Back Parlor.
 */
public interface ParlorService extends InvocationService<PlayerObject>
{
    /** Updates this parlor's parlor configuration. */
    public void updateParlorConfig (ParlorInfo info, boolean creatorStart);

    /** Updates this parlor's password. */
    public void updateParlorPassword (String password);

    /** Requests that a game be located meeting the speified criterion. */
    public void findSaloonMatch (Criterion criterion, ResultListener listener);

    /** Requests that we leave our currently pending match. */
    public void leaveSaloonMatch (int matchOid);

    /** Requests that a player be booted from a back parlor. */
    public void bootPlayer (int bodyOid);
}
