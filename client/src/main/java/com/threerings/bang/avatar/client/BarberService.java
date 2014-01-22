//
// $Id$

package com.threerings.bang.avatar.client;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;

import com.threerings.bang.avatar.data.LookConfig;

/**
 * Provides Barber-related functionality.
 */
public interface BarberService extends InvocationService<PlayerObject>
{
    /**
     * Requests that the specified look be purchased.
     */
    public void purchaseLook (LookConfig look, ConfirmListener cl);

    /**
     * Requests that the specified look be configured with the specified set of
     * articles. No confirmation is provided as the client will have already
     * applied the changes locally.
     *
     * @param name the name of the look to be configured.
     * @param articles item ids of the article inventory items to be used to
     * configure the look.
     */
    public void configureLook (String name, int[] articles);

    /**
     * Requests that a player's handle be changed. This will charge the player
     * the associated handle changing fee.
     */
    public void changeHandle (Handle handle, ConfirmListener cl);
}
