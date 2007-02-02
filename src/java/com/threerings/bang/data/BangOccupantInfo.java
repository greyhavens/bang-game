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
    public AvatarInfo avatar;

    /** The player id */
    public int playerId;

    /** Creates an instance for the specified user. */
    public BangOccupantInfo (PlayerObject user)
    {
        super(user);

        Look look = user.getLook(Look.Pose.DEFAULT);
        if (look != null) {
            avatar = look.getAvatar(user);
        }
        playerId = user.playerId;
    }

    /** Creates a blank instance for unserialization. */
    public BangOccupantInfo ()
    {
    }
}
