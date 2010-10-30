//
// $Id$

package com.threerings.bang.chat.client;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Throttle;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.dobj.MessageEvent;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.gang.data.HideoutObject;

/**
 * Handles custom chat bits for Bang.
 */
public class BangChatDirector extends ChatDirector
{
    public BangChatDirector (BangContext ctx)
    {
        super(ctx, BangCodes.CHAT_MSGS);
        _ctx = ctx;

        // add mute command handlers
        MessageBundle msg = _ctx.getMessageManager().getBundle(_bundle);
        registerCommandHandler(msg, "mute", new CommandHandler() {
            public String handleCommand (
                SpeakService speaksvc, String command, String args,
                String[] history) {
                if (StringUtil.isBlank(args)) {
                    return getUsage(command);
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
                    return getUsage(command);
                }
                Handle name = new Handle(args);
                if (!_ctx.getMuteDirector().isMuted(name)) {
                    return MessageBundle.tcompose("m.not_muted", name);
                }
                _ctx.getMuteDirector().setMuted(name, false);
                return SUCCESS;
            }
        });

        // override our tell handler
        registerCommandHandler(msg, "tell", new TellHandler() {
            protected Name normalizeAsName (String handle) {
                return new Handle(handle);
            }
        });
    }

    @Override // from ChatDirector
    public boolean addChatDisplay (ChatDisplay display)
    {
        // keep system displays at the end of the list as a catch all
        _displays.add(_displays.size() - _systems.size(), display);
        return true;
    }

    @Override // from ChatDirector
    public boolean removeChatDisplay (ChatDisplay display)
    {
        _systems.remove(display);
        return super.removeChatDisplay(display);
    }

    /**
     * Adds the supplied chat display to the end of the chat display list.  It will be kept at
     * the end of the list until another call to addSystemDisplay.
     */
    public void addSystemDisplay (ChatDisplay display)
    {
        super.addChatDisplay(display);
        _systems.add(display);
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
    public void registerCommandHandler (MessageBundle msg, String command, CommandHandler handler)
    {
        // we never want to use the default channel
        if (handler instanceof SpeakHandler) {
            return;
        }
        super.registerCommandHandler(msg, command, handler);
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

    /**
     * Special handling for doing a command while engaged in tell chat.
     */
    public String requestTellCommand (Name target, String msg)
    {
        String command = msg.substring(1).toLowerCase();
        String args = null;
        int sidx = msg.indexOf(" ");
        if (sidx != -1) {
            command = msg.substring(1, sidx).toLowerCase();
            args = msg.substring(sidx+1).trim();
        }

        Map<String,CommandHandler> possibleCommands = getCommandHandlers(command);
        if (args != null && possibleCommands.size() == 1) {
            CommandHandler cmd = possibleCommands.values().iterator().next();
            if (cmd instanceof SpeakHandler || cmd instanceof EmoteHandler ||
                    cmd instanceof ThinkHandler) {
                requestTell(target, args, null);
            }

        }
        return requestChat(null, msg, true);
    }

    @Override // documentation inherited
    public <T extends Name> void requestTell (T target, String msg, ResultListener<T> rl)
    {
        super.requestTell(target, msg, rl);
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
    protected void dispatchPreparedMessage (ChatMessage message)
    {
        super.dispatchPreparedMessage(message);

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

        // while a game is playing, only let the participants chat
        PlaceObject plobj = _ctx.getLocationDirector().getPlaceObject();
        if (plobj instanceof BangObject) {
            BangObject bangobj = (BangObject)plobj;
            if (bangobj.state == BangObject.IN_PLAY &&
                    bangobj.getPlayerIndex(_ctx.getUserObject().handle) == -1) {
                return "e.not_player";
            }
        } else if (plobj instanceof HideoutObject) {
            return "m.internal_error";
        }
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

    /** Any system level displays. */
    protected ArrayList<ChatDisplay> _systems = new ArrayList<ChatDisplay>();

    /** The total number of chat messages we store before dumping the oldest */
    protected static final int MESSAGE_HISTORY_LIMIT = 50;
}
