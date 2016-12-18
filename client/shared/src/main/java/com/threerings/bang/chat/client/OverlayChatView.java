//
// $Id$

package com.threerings.bang.chat.client;

import com.jmex.bui.BComponent;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;

import com.samskivert.util.Interval;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;
import static com.threerings.bang.client.BangMetrics.*;

/**
 * Displays chat within a game.
 */
public class OverlayChatView extends BWindow
    implements ChatDisplay
{
    public OverlayChatView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), GroupLayout.makeVert(
                  GroupLayout.NONE, GroupLayout.BOTTOM, GroupLayout.STRETCH));
        setLayer(2);

        _ctx = ctx;
        _chatdtr = (BangChatDirector)_ctx.getChatDirector();
        _msgs = _ctx.getMessageManager().getBundle(BangCodes.CHAT_MSGS);

        _stamps = new long[CHAT_LINES];
        _history = new MessageLabel[CHAT_LINES];
        for (int ii = 0; ii < _history.length; ii++) {
            add(_history[ii] = new MessageLabel());
        }
        add(_input = new BTextField(BangUI.TEXT_FIELD_MAX_LENGTH) {
            public void render (Renderer renderer) {
                if (hasFocus()) {
                    super.render(renderer);
                }
            }
            protected void layout () {
                super.layout();
                // restore the halted message from the parlor or match view
                // on first layout
                if (!_initialized) {
                    setText(_chatdtr.clearHaltedMessage());
                    setCursorPos(getText().length());
                    if (_cursp > 0) {
                        requestFocus();
                    }
                    _initialized = true;
                }
            }
            protected boolean _initialized;
        });

        _input.addListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                if (handleInput(_input.getText())) {
                    _input.setText("");
                }
            }
        });
    }

    /**
     * Called by the main game view when we enter the game room.
     */
    public void willEnterPlace (PlaceObject plobj)
    {
        _chatdtr.addChatDisplay(this);
        _bangobj = (BangObject)plobj;

        // start our chat expiration timer
        _timer = new Interval(_ctx.getClient().getRunQueue()) {
            public void expired () {
                expireChat();
            }
        };
        _timer.schedule(1000L, true);
    }

    /**
     * Called by the main game view when we leave the game room.
     */
    public void didLeavePlace (PlaceObject plobj)
    {
        _chatdtr.removeChatDisplay(this);
        _timer.cancel();
    }

    /**
     * Determines whether the chat input field has focus.
     */
    public boolean hasFocus ()
    {
        return _input.hasFocus();
    }

    /**
     * Instructs our chat input field to request focus.
     */
    public void requestFocus ()
    {
        _input.requestFocus();
    }

    // documentation inherited from interface ChatDisplay
    public void clear ()
    {
        for (int ii = 0; ii < _history.length; ii++) {
            _history[ii].setText("");
            _stamps[ii] = 0L;
        }
    }

    // documentation inherited from interface ChatDisplay
    public boolean displayMessage (ChatMessage msg, boolean alreadyDisplayed)
    {
        if (alreadyDisplayed) {
            return false;
        }

        if (msg instanceof UserMessage) {
            UserMessage umsg = (UserMessage) msg;
            if (umsg instanceof TellFeedbackMessage) {
                appendTellFeedback(umsg.speaker, umsg.message);
            } else if (umsg.mode == ChatCodes.BROADCAST_MODE) {
                appendBroadcast(umsg.speaker, umsg.message);
            } else if (umsg.localtype == ChatCodes.USER_CHAT_TYPE) {
                appendMessage(umsg.speaker, umsg.message, true);
            } else {
                appendMessage(umsg.speaker, umsg.message, false);
            }
            return true;

        } else if (msg instanceof SystemMessage) {
            appendMessage(msg.message, ColorRGBA.white);
            return true;

        } else {
            log.warning("Received unknown message type: " + msg + ".");
            return false;
        }
    }

    @Override // documentation inherited
    public BComponent getHitComponent (int mx, int my) {
        // accept clicks on the input component when it already has focus
        BComponent hit = super.getHitComponent(mx, my);
        return (hit == _input && _input.hasFocus()) ? hit : null;
    }

    protected void displayError (String message)
    {
        appendMessage(message, ColorRGBA.red);
    }

    protected void appendTellFeedback (Name speaker, String message)
    {
        ColorRGBA color = ColorRGBA.white;
        int pidx;
        if ((pidx = _bangobj.getPlayerIndex(_ctx.getUserObject().handle)) != -1) {
            color = JPIECE_COLORS[colorLookup[pidx + 1]];
        }
        appendMessage(
            _msgs.xlate(MessageBundle.tcompose("m.told_format", speaker, message)), color);
    }

    protected void appendBroadcast (Name speaker, String message)
    {
        ColorRGBA color = ColorRGBA.white;
        appendMessage(
            _msgs.xlate(MessageBundle.tcompose("m.broadcast_format", speaker, message)), color);
    }

    protected void appendMessage (Name speaker, String message, boolean tell)
    {
        ColorRGBA color = ColorRGBA.white;
        int pidx;
        if ((pidx = _bangobj.getPlayerIndex(speaker)) != -1) {
            color = JPIECE_COLORS[colorLookup[pidx + 1]];
        }
        String msg = tell ?
            _msgs.xlate(MessageBundle.tcompose("m.tell_format", speaker, message)) :
            speaker + ": " + message;
        appendMessage(msg, color);
    }

    protected void appendMessage (String text, ColorRGBA color)
    {
        // first scroll any previous messages up
        int lidx = _history.length-1;
        for (int ii = 0; ii < lidx; ii++) {
            _stamps[ii] = _stamps[ii+1];
            _history[ii].inherit(_history[ii+1]);
        }

        // now stuff this message at the bottom
        _history[lidx].setText(text, color);
        _stamps[lidx] = System.currentTimeMillis();
    }

    protected boolean handleInput (String text)
    {
        String errmsg = _chatdtr.requestChat(null, text, true);
        if (errmsg.equals(ChatCodes.SUCCESS)) {
            _ctx.getRootNode().requestFocus(null);
            return true;

        } else {
            displayError(_ctx.xlate(BangCodes.CHAT_MSGS, errmsg));
            return false;
        }
    }

    protected void expireChat ()
    {
        long now = System.currentTimeMillis();
        for (int ii = 0; ii < _history.length; ii++) {
            if (_stamps[ii] != 0L && (now - _stamps[ii]) > CHAT_EXPIRATION) {
                _stamps[ii] = 0L;
                _history[ii].setText("");
            }
        }
    }

    protected static class MessageLabel extends BLabel
    {
        public MessageLabel () {
            super("", "overlay_chat");
        }

        public void setText (String text, ColorRGBA color) {
            _color = color;
            setText(text);
        }

        public void inherit (MessageLabel other) {
            _color = other._color;
            setText(other.getText());
        }

        @Override // documentation inherited
        public void setText(String text) {
            // filter out escape @s
            String sanitized = text;
            if (text.length() > 4) {
                sanitized = text.substring(0, text.length() - 4).replaceAll("@", "@@") +
                    text.substring(text.length() - 4);
            }
            super.setText(sanitized);
        }

        @Override // documentation inherited
        public ColorRGBA getColor () {
            return _color;
        }

        protected ColorRGBA _color = ColorRGBA.white;
    }

    protected BangContext _ctx;
    protected BangChatDirector _chatdtr;
    protected BangObject _bangobj;
    protected MessageBundle _msgs;

    protected BTextField _input;

    protected Interval _timer;
    protected MessageLabel[] _history;
    protected long[] _stamps;

    protected static final int CHAT_LINES = 5;
    protected static final long CHAT_EXPIRATION = 20 * 1000L;
}
