//
// $Id$

package com.threerings.bang.avatar.client;

import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;

import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.data.LookConfig;

/**
 * Provides avatar invocation services that are available anywhere, not just
 * when in the Barber Shop.
 */
public interface AvatarService extends InvocationService<PlayerObject>
{
    /**
     * Configures a player's avatar for the first time, specifying their handle
     * (in-game name), sex, and default avatar look.
     */
    public void createAvatar (Handle handle, boolean isMale, LookConfig look, int zations,
                              ConfirmListener cl);

    /**
     * Selects the specified look as the player's "current" look.
     *
     * @param name the name of the look to be selected.
     */
    public void selectLook (Look.Pose pose, String name);
}
