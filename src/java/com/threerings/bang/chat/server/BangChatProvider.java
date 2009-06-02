//
// $Id$

package com.threerings.bang.chat.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.util.Name;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.crowd.chat.client.ChatService;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.chat.server.ChatProvider;
import com.threerings.crowd.data.BodyObject;

import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.chat.data.PlayerMessage;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.StatType;
import com.threerings.bang.server.BangServer;

/**
 * Extends the normal {@link ChatProvider} so that we can avatar information along with our tells.
 */
@Singleton
public class BangChatProvider extends ChatProvider
{
    @Inject public BangChatProvider (InvocationManager invmgr)
    {
        super(invmgr);
    }

    @Override // documentation inherited
    public void tell (ClientObject caller, Name target, String message,
                      ChatService.TellListener listener)
        throws InvocationException
    {
        // make sure the message passes the whitelist
        if (_chatmgr.validateChat(caller, message)) {
            super.tell(caller, target, message, listener);
        }
    }

    @Override // documentation inherited
    public void deliverTell (BodyObject target, UserMessage message)
    {
        PlayerObject user = (PlayerObject)target;
        user.stats.incrementStat(StatType.CHAT_RECEIVED, 1);

        user = (PlayerObject)BangServer.locator.lookupBody(message.speaker);
        if (user != null) {
            user.stats.incrementStat(StatType.CHAT_SENT, 1);
        }

        super.deliverTell(target, message);
    }

    @Override // documentation inherited
    protected UserMessage createTellMessage (BodyObject source, String message)
    {
        PlayerObject player = (PlayerObject)source;
        AvatarInfo avatar = player.getLook(Look.Pose.DEFAULT).getAvatar(player);
        return new PlayerMessage(player.handle, avatar, message);
    }

    @Inject protected BangChatManager _chatmgr;
}
