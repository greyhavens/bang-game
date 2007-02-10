//
// $Id$

package com.threerings.bang.chat.client;


import java.util.LinkedList;
import java.util.List;

import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Throttle;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.dobj.MessageEvent;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
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

    /**
     * Initialize the chat throttle.
     */
    public void checkClientThrottle ()
    {
        // if we're a support user, turn off the throttle
        PlayerObject user = _ctx.getUserObject();
        if (user != null && user.tokens.isSupport()) {
            _chatThrottle = new Throttle(1, 0);
        }
    }

    /**
     * Stores any message being composed when a chat entry field disappears.
     */
    public void setHaltedMessage (String msg)
    {
        _haltedMessage = msg;
    }

    /**
     * Clears and returns the stored halted message.
     */
    public String clearHaltedMessage ()
    {
        String msg = _haltedMessage;
        _haltedMessage = "";
        return msg;
    }

    /**
     * Returns the most recently received chats. Do not modify this value!
     */
    public List<ChatMessage> getMessageHistory ()
    {
        return _messageHistory;
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
        // we override messageReceived() here rather than dispatchMessage()
        // because we only want to make noise when a message comes in over the
        // network not when things like tell feedback are dispatched locally
        if (CHAT_NOTIFICATION.equals(event.getName())) {
            ChatMessage msg = (ChatMessage)event.getArgs()[0];
            Name speaker = null;
            if (msg instanceof UserMessage) {
                speaker = ((UserMessage)msg).speaker;
            }

            // don't play sounds from muted speakers
            if (speaker == null || !_ctx.getMuteDirector().isMuted(speaker)) {
                // for now all incoming chat messages have the same sound; maybe
                // we'll want special sounds for special messages later
                BangUI.play(BangUI.FeedbackSound.CHAT_RECEIVE);
            }
        }
        super.messageReceived(event);
    }

    @Override // documentation inherited
    public void dispatchMessage (ChatMessage message)
    {
        super.dispatchMessage(message);

        // store the message in our history
        _messageHistory.add(message);
        if (_messageHistory.size() > MESSAGE_HISTORY_LIMIT) {
            _messageHistory.remove(0);
        }
    }

    @Override // documentation inherited
    protected String checkCanChat (SpeakService speakSvc, String message, byte mode)
    {
        // if we're speaking on a particular channel, just let it through
        if (speakSvc != null) {
            return null;
        }

        // make sure their voice isn't going hoarse
        long now = System.currentTimeMillis();
        if (_chatThrottle.wouldThrottle(now)) {
            return "e.too_chatty";
        }
        _chatThrottle.noteOp(now);
        return null;
    }

    /** Provides acces to client services. */
    protected BangContext _ctx;

    /** The most recent chat messages we've received */
    protected List<ChatMessage> _messageHistory = new LinkedList<ChatMessage>();

    /** The text of any message being composed on the client when the last chat
     * entry field disappeared. */
    protected String _haltedMessage = "";

    /** We throttle chat for non support users. */
    protected Throttle _chatThrottle = new Throttle(4, 10000);

    /** The total number of chat messages we store before dumping the oldest */
    protected static final int MESSAGE_HISTORY_LIMIT = 50;
}
