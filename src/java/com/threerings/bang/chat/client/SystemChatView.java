//
// $Id$

package com.threerings.bang.chat.client;

import com.jme.scene.Controller;

import com.jmex.bui.BComponent;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.util.MessageBundle;

import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.bang.client.MainView;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

/**
 * Displays notifications for system messages outside of games.
 */
public class SystemChatView extends BWindow
    implements ChatDisplay, ChatCodes, BangCodes
{
    /**
     * Returns a string describing the system attention level of the given message, or
     * <code>null</code> for none.
     */
    public static String getAttentionLevel (ChatMessage msg)
    {
        if (msg instanceof TellFeedbackMessage) {
            return "feedback";
        }
        if (msg instanceof UserMessage) {
            return ((UserMessage)msg).mode == BROADCAST_MODE ? "attention" : null;
        }
        if (!(msg instanceof SystemMessage)) {
            return null;
        }
        SystemMessage smsg = (SystemMessage)msg;
        if (smsg.attentionLevel == SystemMessage.ATTENTION) {
            return "attention";
        } else if (smsg.attentionLevel == SystemMessage.FEEDBACK) {
            return "feedback";
        } else { // smsg.attentionLevel == SystemMessage.INFO) {
            return "info";
        }
    }

    /**
     * Formats the given system message.
     */
    public static String format (BangContext ctx, ChatMessage msg)
    {
        if (!(msg instanceof UserMessage) || msg instanceof TellFeedbackMessage) {
            return msg.message;
        }
        UserMessage umsg = (UserMessage)msg;
        return ctx.xlate(CHAT_MSGS, MessageBundle.tcompose(
            "m.broadcast_format", umsg.speaker, msg.message));
    }

    public SystemChatView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), new TableLayout(3, 20, 20));
        setStyleClass("system_chat_view");
        _ctx = ctx;
        ((BangChatDirector)_ctx.getChatDirector()).addSystemDisplay(this);
        setBounds(0, 0, ctx.getDisplay().getWidth(), ctx.getDisplay().getHeight());
        setLayer(2);
    }

    @Override // we never want the chat window to accept clicks
    public BComponent getHitComponent (int mx, int my) {
        return null;
    }

    @Override // documentation inherited
    public boolean isOverlay ()
    {
        return true;
    }

    // documentation inherited from interface ChatDisplay
    public boolean displayMessage (ChatMessage msg, boolean alreadyDisplayed)
    {
        if (alreadyDisplayed) {
            return false;
        }

        String level = getAttentionLevel(msg);
        if (level == null || !_ctx.getBangClient().canDisplayPopup(MainView.Type.SYSTEM) ||
                (!msg.localtype.equals(ChatCodes.PLACE_CHAT_TYPE) && "feedback".equals(level))) {
            return false;
        }
        if (!isAdded()) {
            _ctx.getRootNode().addWindow(this);
            _ctx.getRootNode().addController(_fctrl);
        }
        add(new MessageLabel(format(_ctx, msg), level + "_chat_label"));
        return true;
    }

    // documentation inherited from interface ChatDisplay
    public void clear ()
    {
        if (isAdded()) {
            removeAll();
            _ctx.getRootNode().removeWindow(this);
        }
        _ctx.getRootNode().removeController(_fctrl);
    }

    /**
     * Checks to see if we should be re-added to the root node.
     */
    public void maybeShow ()
    {
        if (!isAdded() && getComponentCount() > 0) {
            _ctx.getRootNode().addWindow(this);
        }
    }

    /** A label displaying a single message. */
    protected class MessageLabel extends BLabel
    {
        public MessageLabel (String text, String styleClass) {
            super(text, styleClass);
        }

        /**
         * Updates the alpha value of this label.
         *
         * @return true if the label is still showing, false if it has completely vanished
         */
        public boolean updateAlpha (float time) {
            if (_elapsed >= MESSAGE_LINGER_DURATION + MESSAGE_FADE_DURATION) {
                _alpha = 0f;
            } else if (_elapsed > MESSAGE_LINGER_DURATION) {
                _alpha  = 1f - (_elapsed - MESSAGE_LINGER_DURATION) / MESSAGE_FADE_DURATION;
            } else {
                _alpha  = 1f;
            }
            _elapsed += time;
            return _alpha > 0f;
        }

        protected Dimension computePreferredSize (int whint, int hhint) {
            return super.computePreferredSize(308, hhint);
        }

        protected float _elapsed;
    }

    /** Fades out the labels on the screen. */
    protected Controller _fctrl = new Controller() {
        public void update (float time) {
            boolean anyShowing = false;
            for (int ii = 0, nn = getComponentCount(); ii < nn; ii++) {
                anyShowing = anyShowing || ((MessageLabel)getComponent(ii)).updateAlpha(time);
            }
            if (!anyShowing) {
                clear();
            }
        }
    };

    /** Giver of life and context. */
    protected BangContext _ctx;

    /** The amount of time for which messages linger on the screen. */
    protected static final float MESSAGE_LINGER_DURATION = 10f;

    /** The amount of time it takes for messages to fade out. */
    protected static final float MESSAGE_FADE_DURATION = 1f;
}
