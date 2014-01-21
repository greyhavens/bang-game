//
// $Id$

package com.threerings.bang.data;

import com.threerings.crowd.peer.data.CrowdClientInfo;

/**
 * Extends the peer server client info to contain an avatar.
 */
public class BangClientInfo extends CrowdClientInfo
{
    /** The player's unique identifier. */
    public int playerId;

    /** This player's avatar (used for pardner purposes). */
    public AvatarInfo avatar;
}
