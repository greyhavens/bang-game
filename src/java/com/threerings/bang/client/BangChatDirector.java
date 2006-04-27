//
// $Id$

package com.threerings.bang.client;

import com.samskivert.util.ResultListener;
import com.threerings.util.Name;

import com.threerings.presents.dobj.MessageEvent;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.SpeakService;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

/**
 * Handles custom chat bits for Bang.
 */
public class BangChatDirector extends ChatDirector
{
    public BangChatDirector (BangContext ctx)
    {
        super(ctx, ctx.getMessageManager(), BangCodes.CHAT_MSGS);
        _ctx = ctx;
        // nothing currently doing, but eventually we'll have something
    }

    @Override // documentation inherited
    public String requestChat (
        SpeakService speakSvc, String text, boolean record)
    {
        String rv = super.requestChat(speakSvc, text, record);
        if (rv == null) {
            BangUI.play(BangUI.FeedbackSound.CHAT_SEND);
        }
        return rv;
    }

    @Override // documentation inherited
    public void requestTell (Name target, String msg, ResultListener rl)
    {
        super.requestTell(target, msg, rl);
        BangUI.play(BangUI.FeedbackSound.CHAT_SEND);
    }

    @Override // documentation inherited
    public void messageReceived (MessageEvent event)
    {
        if (CHAT_NOTIFICATION.equals(event.getName())) {
            // for now all incoming chat messages have the same sound; maybe
            // we'll want special sounds for special messages later
            BangUI.play(BangUI.FeedbackSound.CHAT_RECEIVE);
        }
        super.messageReceived(event);
    }

    protected BangContext _ctx;
}
