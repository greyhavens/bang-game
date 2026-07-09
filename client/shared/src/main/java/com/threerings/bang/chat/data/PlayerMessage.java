//
// $Id$

package com.threerings.bang.chat.data;

import com.threerings.util.Name;

import com.threerings.crowd.chat.data.ChatCodes;
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
     * Constructs a message for a player originated tell.
     */
    public PlayerMessage (Name speaker, AvatarInfo avatar, String message)
    {
        super(speaker, null, message, ChatCodes.DEFAULT_MODE);
        this.avatar = avatar;
    }

    // from interface Streamable
    public void readObject (com.threerings.io.ObjectInputStream ins)
        throws java.io.IOException, java.lang.ClassNotFoundException
    {
        super.readObject(ins);
        com.threerings.io.GenStreamUtil.readField(com.threerings.bang.chat.data.PlayerMessage.class, "avatar", this, ins);
    }

    // from interface Streamable
    public void writeObject (com.threerings.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        super.writeObject(out);
        com.threerings.io.GenStreamUtil.writeField(com.threerings.bang.chat.data.PlayerMessage.class, "avatar", this, out);
    }
}
