//
// $Id$

package com.threerings.bang.avatar.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.Handle;

/**
 * Provides avatar invocation services that are available anywhere, not just
 * when in the Barber Shop.
 */
public interface AvatarService extends InvocationService
{
    /**
     * Configures a player's avatar for the first time, specifying their handle
     * (in-game name), sex, and default avatar look.
     */
    public void createAvatar (
        Client client, Handle handle, boolean isMale, int hair, int skin,
        String[] aspects, int[] colors, ConfirmListener cl);

    /**
     * Selects the specified look as the player's "current" look.
     *
     * @param name the name of the look to be selected.
     */
    public void selectLook (Client client, String name);
}
