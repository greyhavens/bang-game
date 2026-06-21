//
// $Id$

package com.threerings.bang.chat.client;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;

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
        setBounds(0, 0, ctx.getCamera().getWidth(), ctx.getCamera().getHeight());
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
            startFader();
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
        stopFader();
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

    /**
     * Starts the per-frame fade driver if it isn't already running.
     *
     * <p>jME3 cutover: the fork attached {@code _fctrl} as a {@code com.jme.scene.Controller} to
     * the BUI root node ({@code addController}). jME3's {@code BRootNode} is no longer a scene node
     * and has no controller hook, so this follows the migrated {@code WindowFader} idiom: a jME3
     * {@link Node} attached to {@code ctx.getInterface()} carrying an {@link AbstractControl} that
     * steps the label alpha each frame and self-detaches when nothing is left showing.
     */
    protected void startFader ()
    {
        if (_fader == null) {
            _fader = new Node("system_chat_fader");
            _fader.addControl(new AbstractControl() {
                @Override protected void controlUpdate (float tpf) {
                    boolean anyShowing = false;
                    for (int ii = 0, nn = getComponentCount(); ii < nn; ii++) {
                        anyShowing = ((MessageLabel)getComponent(ii)).updateAlpha(tpf) ||
                            anyShowing;
                    }
                    if (!anyShowing) {
                        clear();
                    }
                }
                @Override protected void controlRender (RenderManager rm, ViewPort vp) {
                }
            });
        }
        if (_fader.getParent() == null) {
            _ctx.getInterface().attachChild(_fader);
        }
    }

    /** Detaches the per-frame fade driver. */
    protected void stopFader ()
    {
        if (_fader != null && _fader.getParent() != null) {
            _fader.getParent().detachChild(_fader);
        }
    }

    /** Giver of life and context. */
    protected BangContext _ctx;

    /** The interface-attached node whose control fades out the labels each frame. */
    protected Node _fader;

    /** The amount of time for which messages linger on the screen. */
    protected static final float MESSAGE_LINGER_DURATION = 10f;

    /** The amount of time it takes for messages to fade out. */
    protected static final float MESSAGE_FADE_DURATION = 1f;
}
