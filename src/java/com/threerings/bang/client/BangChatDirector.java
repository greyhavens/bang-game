//
// $Id$

package com.threerings.bang.client;

import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.dobj.MessageEvent;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.SpeakService;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
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
        
        // add mute command handlers
        MessageBundle msg = _msgmgr.getBundle(_bundle);
        registerCommandHandler(msg, "mute", new CommandHandler() {
            public String handleCommand (
                SpeakService speaksvc, String command, String args,
                String[] history) {
                if (StringUtil.isBlank(args)) {
                    return "m.usage_mute";
                }
                Handle name = new Handle(args);
                if (_ctx.getMuteDirector().isMuted(name)) {
                    return MessageBundle.tcompose("m.already_muted", name);
                }
                _ctx.getMuteDirector().setMuted(name, true);
                return SUCCESS;
            }
        });
        registerCommandHandler(msg, "unmute", new CommandHandler() {
            public String handleCommand (
                SpeakService speaksvc, String command, String args,
                String[] history) {
                if (StringUtil.isBlank(args)) {
                    return "m.usage_unmute";
                }
                Handle name = new Handle(args);
                if (!_ctx.getMuteDirector().isMuted(name)) {
                    return MessageBundle.tcompose("m.not_muted", name);
                }
                _ctx.getMuteDirector().setMuted(name, false);
                return SUCCESS;
            }
        });
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
    public void requestTell (Name target, String msg, ResultListener<Name> rl)
    {
        // we need to convert Name to Handle so that things are properly
        // dispatched on the server
        Handle thandle = new Handle(target.toString());
        super.requestTell(thandle, msg, rl);
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
