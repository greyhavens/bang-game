//
// $Id$

package com.threerings.bang.ranch.client;

import com.threerings.bang.data.PlayerObject;
import com.threerings.presents.client.InvocationService;

import com.threerings.util.Name;

/**
 * Provides ranch-related functionality.
 */
public interface RanchService extends InvocationService<PlayerObject>
{
    /**
     * Requests that a big shot of the specified type be recruited onto
     * the player's ranch.
     */
    public void recruitBigShot (String type, Name name, ResultListener listener);
}
