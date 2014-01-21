//
// $Id$

package com.threerings.bang.chat.client;

import java.util.HashMap;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.chat.data.PlayerMessage;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.client.bui.TabbedPane;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * The common superclass for displaying user-to-user tells in a tabbed
 * view that shows the users' avatars next to the text.
 */
public class TabbedChatView extends BContainer
    implements ActionListener, ChatDisplay
{
    public TabbedChatView (BangContext ctx, Dimension tabSize)
    {
        super(GroupLayout.makeVStretch());

        _ctx = ctx;
        _tabSize = tabSize;

        // we want to be first to hear about chat messages
        _ctx.getChatDirector().pushChatDisplay(this);

        add(_pane = new TabbedPane(true) {
            public void selectTab (int tabidx) {
                super.selectTab(tabidx);
                // hide and disable our input elements if we're on a non-chat tab
                boolean visible = (getSelectedTab() instanceof ChatTab);
                _input.setVisible(visible);
                _input.setEnabled(visible);
                _send.setVisible(visible);
                if (!visible) {
                    _send.setEnabled(visible);
                } else {
                    _send.setEnabled(EnablingValidator.validate(_input.getText()));
                }
            }
            protected void tabWasRemoved (BComponent tab, boolean btnClose) {
                super.tabWasRemoved(tab, btnClose);
                if (btnClose) {
                    ((UserTab)tab).wasClosed();
                }
                if (getTabCount() == 0) {
                    lastTabClosed();
                }
            }
        });

        _pane.addListener(this);
        BContainer tcont = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.STRETCH, GroupLayout.CENTER, GroupLayout.NONE));
        tcont.add(_input = new BTextField(BangUI.TEXT_FIELD_MAX_LENGTH));
        _input.addListener(this);
        ImageIcon icon = new ImageIcon(_ctx.loadImage("ui/chat/bubble_icon.png"));
        tcont.add(_send = new BButton(icon, this, "send"), GroupLayout.FIXED);
        add(tcont, GroupLayout.FIXED);

        // disable send until some text is entered
        new EnablingValidator(_input, _send);

        _alert = new ImageIcon(_ctx.loadImage("ui/chat/alert_icon.png"));
    }

    /**
     * This must be called when we are leaving the room in which this chat view is displaying chat.
     */
    public void shutdown ()
    {
        _ctx.getChatDirector().removeChatDisplay(this);
    }

    // from interface ChatDisplay
    public void clear ()
    {
        if (!_input.hasFocus() && !_send.hasFocus()) {
            return;
        }
        Object tab = _pane.getSelectedTab();
        if (tab instanceof ChatTab) {
            ((ChatTab)tab).clear();
        }
    }

    // from interface ChatDisplay
    public boolean displayMessage (ChatMessage msg, boolean alreadyDisplayed)
    {
        if (isAdded() && // don't intercept feedback if we're not showing
            msg instanceof TellFeedbackMessage && // we also have to handle tell failures
            ((TellFeedbackMessage)msg).isFailure()) {
            Object tab = _pane.getSelectedTab();
            if (tab instanceof UserTab) {
                ((UserTab)tab).appendSystem(msg);
                return true;
            }
        }
        return false;
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent ae)
    {
        Object src = ae.getSource();
        if (src == _send || (src == _input && _send.isEnabled())) {
            String msg = _input.getText().trim();
            _input.setText("");
            ((UserTab)_pane.getSelectedTab()).requestTell(msg);
        }
    }

    /**
     * Allows subclasses to bring the chat interface to front if it is hidden and a chat message
     * arrives.
     *
     * @return true if the tabs were brought to front, false if that was not possible.
     */
    protected boolean displayTabs ()
    {
        return false;
    }

    /**
     * Lets subclasses react to the last tab closing.
     */
    protected void lastTabClosed ()
    {
    }

    /**
     * Handles the chat display for single user.
     */
    protected class UserTab extends ComicChatView
    {
        public UserTab (BangContext ctx, Handle user)
        {
            super(ctx, _tabSize, false);
            _user = user;
        }

        /**
         * Change the user avatar.
         */
        public void setAvatar (AvatarInfo avatar)
        {
            _avatar = avatar;
        }

        /**
         * Attempts to send a tell to this tab's user.
         */
        public void requestTell (final String msg)
        {
            if (msg.startsWith("/")) {
                String error = ((BangChatDirector)_ctx.getChatDirector()).requestTellCommand(
                    _user, msg);
                if (!ChatCodes.SUCCESS.equals(error)) {
                    SystemMessage sysmsg = new SystemMessage(
                        _ctx.xlate(BangCodes.CHAT_MSGS, error), null, SystemMessage.FEEDBACK);
                    appendSystem(sysmsg);
                }
            } else {
                _ctx.getChatDirector().requestTell(_user, msg, null);
            }
        }

        /**
         * Mutes this tab's user.
         */
        public void mute ()
        {
            if (_myPane != null) {
                _myPane.removeTab(this);
            }
            _ctx.getMuteDirector().setMuted(_user, true);
        }

        /**
         * Called when the tab was closed so it can remove itself.
         */
        public void wasClosed ()
        {
            _users.remove(_user);
        }

        /**
         * Called to set the tabbed pane this tab is in.
         */
        public void setTabbedPane (TabbedPane pane)
        {
            _myPane = pane;
        }

        @Override // documentation inherited
        public void appendReceived (UserMessage msg)
        {
            // set/update the avatar before appending
            if (msg instanceof PlayerMessage && msg.speaker.equals(_user)) {
                _avatar = ((PlayerMessage)msg).avatar;
            }
            super.appendReceived(msg);
        }

        @Override // documentation inherited
        protected void wasAdded ()
        {
            super.wasAdded();

            // clear the alert icon, if present
            if (_myPane != null) {
                BButton btn = _myPane.getTabButton(this);
                if (btn != null) {
                    btn.setIcon(null);
                }
            }
        }

        @Override // documentation inherited
        protected AvatarInfo getSpeakerAvatar (Handle speaker)
        {
            if (speaker.equals(_ctx.getUserObject().handle)) {
                PlayerObject player = _ctx.getUserObject();
                Look look = player.getLook(Look.Pose.DEFAULT);
                return (look == null) ? null : look.getAvatar(player);

            } else if (speaker.equals(_user)) {
                return _avatar;

            } else {
                // this should never happen
                log.warning("Unknown speaker", "speaker", speaker, "user", _user);
                return null;
            }
        }

        protected Handle _user;
        protected AvatarInfo _avatar;
        protected TabbedPane _myPane;
    }

    protected BangContext _ctx;
    protected BTextField _input;
    protected BButton _send;
    protected TabbedPane _pane;
    protected Dimension _tabSize;
    protected BIcon _alert;

    protected HashMap<Handle,UserTab> _users = new HashMap<Handle,UserTab>();
}
