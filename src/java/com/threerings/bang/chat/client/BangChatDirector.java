//
// $Id$

package com.threerings.bang.chat.client;


import java.util.LinkedList;
import java.util.List;

import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.dobj.MessageEvent;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatMessage;

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
    
    /** Returns the most recently received chats. Do not modify this value! */
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
        if (CHAT_NOTIFICATION.equals(event.getName())) {
            // for now all incoming chat messages have the same sound; maybe
            // we'll want special sounds for special messages later
            BangUI.play(BangUI.FeedbackSound.CHAT_RECEIVE);
            // store the message in our history
            ChatMessage msg = (ChatMessage) event.getArgs()[0];
            _messageHistory.add(msg);
            if (_messageHistory.size() > MESSAGE_HISTORY_LIMIT) {
                _messageHistory.remove(0);
            }
        }
        super.messageReceived(event);
    }

    /** The total number of chat messages we store before dumping the oldest */
    protected static final int MESSAGE_HISTORY_LIMIT = 50;

    /** The most recent chat messages we've received */
    protected List<ChatMessage> _messageHistory = new LinkedList<ChatMessage>();

    protected BangContext _ctx;
    
    /** The text of any message being composed on the client when the last chat
     * entry field disappeared. */
    protected String _haltedMessage = "";
}
