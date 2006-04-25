//
// $Id$

package com.threerings.bang.data;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.bang.avatar.data.Look;

/**
 * Extends the standard occupant info for a Bang! player.
 */
public class BangOccupantInfo extends OccupantInfo
{
    /** The player's avatar definition. */
    public int[] avatar;

    /** Creates an instance for the specified user. */
    public BangOccupantInfo (PlayerObject user)
    {
        super(user);

        Look look = user.getLook(Look.Pose.DEFAULT);
        if (look != null) {
            avatar = look.getAvatar(user);
        }
    }

    /** Creates a blank instance for unserialization. */
    public BangOccupantInfo ()
    {
    }
}
