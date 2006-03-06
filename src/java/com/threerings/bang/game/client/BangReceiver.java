//
// $Id$

package com.threerings.bang.game.client;

import com.threerings.presents.client.InvocationReceiver;

/**
 * Provides a mechanism for the game manager to contact the client
 * asynchronously during a game.
 */
public interface BangReceiver extends InvocationReceiver
{
    /**
     * Informs the client that the advance order for the specified unit has
     * become invalid.
     */
    public void orderInvalidated (int unitId, String reason);
}
