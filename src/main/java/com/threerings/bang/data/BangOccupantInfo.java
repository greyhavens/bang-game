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

    /** The player's gang id. */
    public int gangId;

    /** Creates {@link AvatarInfo} for the supplied player. */
    public static AvatarInfo getAvatar (PlayerObject user)
    {
        Look look = user.getLook(Look.Pose.DEFAULT);
        return (look == null) ? null : look.getAvatar(user);
    }

    /** Creates an instance for the specified user. */
    public BangOccupantInfo (PlayerObject user)
    {
        super(user);

        avatar = getAvatar(user);
        playerId = user.playerId;
        gangId = user.gangId;
    }

    /** Creates a blank instance for unserialization. */
    public BangOccupantInfo ()
    {
    }
}
