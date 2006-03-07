//
// $Id$

package com.threerings.bang.client;

import java.util.ArrayList;

import com.jme.scene.Controller;

import com.jmex.bui.BComponent;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.bang.util.BangContext;

/**
 * Displays notifications for system messages outside of games.
 */
public class SystemChatView extends BWindow
    implements ChatDisplay, ChatCodes
{
    public SystemChatView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), new TableLayout(3, 20, 20));
        setStyleClass("system_chat_view");
        _ctx = ctx;
        _ctx.getChatDirector().addChatDisplay(this);
        
        setBounds(0, 0, ctx.getDisplay().getWidth(),
                ctx.getDisplay().getHeight());
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
    public void displayMessage (ChatMessage msg)
    {
        if (!(msg instanceof SystemMessage) ||
            _ctx.getBangClient().getPardnerChatView().isAdded() ||
            !_ctx.getBangClient().canDisplayPopup(MainView.Type.SYSTEM)) {
            return;
        }
        if (!isAdded()) {
            _ctx.getRootNode().addWindow(this);
            _ctx.getRootNode().addController(_fctrl);
        }
        SystemMessage smsg = (SystemMessage)msg;
        String level;
        if (smsg.attentionLevel == SystemMessage.ATTENTION) {
            level = "attention";
        } else if (smsg.attentionLevel == SystemMessage.FEEDBACK) {
            level = "feedback";
        } else { // smsg.attentionLevel == SystemMessage.INFO) {
            level = "info";
        }
        add(new MessageLabel(smsg.message, level + "_chat_label"));
    }

    // documentation inherited from interface ChatDisplay
    public void clear ()
    {
        if (isAdded()) {
            removeAll();
            _ctx.getRootNode().removeWindow(this);
            _ctx.getRootNode().removeController(_fctrl);
        }
    }
    
    /** A label displaying a single message. */
    protected class MessageLabel extends BLabel
    {
        public MessageLabel (String text, String styleClass)
        {
            super(text, styleClass);
        }
        
        /**
         * Updates the alpha value of this label.
         *
         * @return true if the label is still showing, false if it has
         * completely vanished
         */
        public boolean updateAlpha (float time)
        {
            if (_elapsed >= MESSAGE_LINGER_DURATION + MESSAGE_FADE_DURATION) {
                _alpha = 0f;
            } else if (_elapsed > MESSAGE_LINGER_DURATION) {
                _alpha  = 1f - (_elapsed - MESSAGE_LINGER_DURATION) /
                    MESSAGE_FADE_DURATION;
            } else {
                _alpha  = 1f;
            }
            _elapsed += time;
            return _alpha > 0f;
        }
        
        protected Dimension computePreferredSize (int whint, int hhint)
        {
            return super.computePreferredSize(308, hhint);
        }
        
        protected float _elapsed;
    }
    
    protected BangContext _ctx;
    
    /** Fades out the labels on the screen. */
    protected Controller _fctrl = new Controller() {
        public void update (float time) {
            boolean anyShowing = false;
            for (int ii = 0, nn = getComponentCount(); ii < nn; ii++) {
                anyShowing = anyShowing ||
                    ((MessageLabel)getComponent(ii)).updateAlpha(time);
            }
            if (!anyShowing) {
                clear();
            }
        }
    };
    
    /** The amount of time for which messages linger on the screen. */
    protected static final float MESSAGE_LINGER_DURATION = 10f;
    
    /** The amount of time it takes for messages to fade out. */
    protected static final float MESSAGE_FADE_DURATION = 1f;
}
