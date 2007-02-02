//
// $Id$

package com.threerings.bang.chat.server;

import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.chat.server.ChatProvider;
import com.threerings.crowd.data.BodyObject;

import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.chat.data.PlayerMessage;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.PlayerObject;

/**
 * Extends the normal {@link ChatProvider} so that we can avatar information
 * along with our tells.
 */
public class BangChatProvider extends ChatProvider
{
    @Override // documentation inherited
    protected UserMessage createTellMessage (BodyObject source, String message)
    {
        PlayerObject player = (PlayerObject)source;
        AvatarInfo avatar = player.getLook(Look.Pose.DEFAULT).getAvatar(player);
        return new PlayerMessage(player.handle, avatar, message);
    }
}
