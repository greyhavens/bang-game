//
// $Id$

package com.threerings.bang.chat.data;

import com.threerings.util.Name;

import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.bang.data.AvatarInfo;

/**
 * Extends the {@link UserMessage} with avatar information. This is used for
 * tells.
 */
public class PlayerMessage extends UserMessage
{
    /** The avatar information for the speaker. */
    public AvatarInfo avatar;

    /**
     * For unserialization.
     */
    public PlayerMessage ()
    {
    }

    /**
     * Constructs a message for a player originated tell.
     */
    public PlayerMessage (Name speaker, AvatarInfo avatar, String message)
    {
        super(speaker, message);
        this.avatar = avatar;
    }
}
