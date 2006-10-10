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

import com.samskivert.util.ResultListener;
import com.threerings.util.Name;

import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;

import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.chat.data.PlayerMessage;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.client.bui.TabbedPane;
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
            protected void tabWasRemoved (BComponent tab) {
                Handle tabOwner = ((UserTab) tab)._user;
                _users.remove(tabOwner);
                if (getTabCount() == 0) {
                    lastTabClosed();
                }
            }
        });

        _pane.addListener(this);
        BContainer tcont = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.STRETCH, GroupLayout.CENTER,
                                  GroupLayout.NONE));
        tcont.add(_input = new BTextField(BangUI.TEXT_FIELD_MAX_LENGTH));
        _input.addListener(this);
        ImageIcon icon = new ImageIcon(
            _ctx.loadImage("ui/chat/bubble_icon.png"));
        tcont.add(_send = new BButton(icon, this, "send"), GroupLayout.FIXED);
        add(tcont, GroupLayout.FIXED);

        // disable send until some text is entered
        new EnablingValidator(_input, _send);

        _alert = new ImageIcon(_ctx.loadImage("ui/chat/alert_icon.png"));
    }

    // from interface ChatDisplay
    public void clear ()
    {
        _pane.removeAllTabs();
        _users.clear();
        _input.setText("");
    }

    // from interface ChatDisplay
    public boolean displayMessage (ChatMessage msg, boolean alreadyDisplayed)
    {
        // if the message was already displayed, we don't do anything
        if (alreadyDisplayed) {
            return false;
        }

        // we handle player-to-player chat
        if (msg instanceof PlayerMessage &&
            ChatCodes.USER_CHAT_TYPE.equals(msg.localtype)) {
            PlayerMessage pmsg = (PlayerMessage)msg;
            Handle handle = (Handle)pmsg.speaker;
            UserTab tab = openUserTab(handle, pmsg.avatar, false);
            if (tab == null) {
                return false;
            }
            if (tab != _pane.getSelectedTab()) {
                _pane.getTabButton(tab).setIcon(_alert);
            }
            tab.appendReceived(pmsg);
            return true;

        } else if (msg instanceof SystemMessage && SystemMessage.FEEDBACK ==
                   ((SystemMessage)msg).attentionLevel) {
            // we also have to handle feedback messages because that's how tell
            // failures are reported
            UserTab tab = (UserTab)_pane.getSelectedTab();
            if (tab != null) {
                tab.appendSystem(msg);
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
            if (msg.startsWith("/")) {
                String error = _ctx.getChatDirector().requestChat(
                    null, msg, true);
                if (!ChatCodes.SUCCESS.equals(error)) {
                    SystemMessage sysmsg = new SystemMessage(
                        _ctx.xlate(BangCodes.CHAT_MSGS, error),
                        null, SystemMessage.FEEDBACK);
                    ((UserTab)_pane.getSelectedTab()).appendSystem(sysmsg);
                }

            } else {
                ((UserTab)_pane.getSelectedTab()).requestTell(msg);
            }
        }
    }

    /**
     * Ensure that a given user tab exists, possibly creating it.
     */
    public UserTab openUserTab (Handle handle, int[] avatar, boolean focus)
    {
        UserTab tab = _users.get(handle);
        if (tab == null) {
            tab = new UserTab(_ctx, handle, avatar);
            _pane.addTab(handle.toString(), tab, true);
            _users.put(handle, tab);
        }
        _pane.selectTab(tab);

        // this has to be called when the tab is already added
        if (!isAdded()) {
            if (!displayTabs()) {
                return null;
            }
        }

        if (focus) {
            _input.requestFocus();
        }
        return tab;
    }

    /**
     * Allows subclasses to bring the chat interface to front if it is hidden
     * and a chat message arrives.
     *
     * @return true if the tabs were brought to front, false if that was not
     * possible.
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
        public UserTab (BangContext ctx, Handle user, int[] avatar)
        {
            super(ctx, _tabSize, false);
            _user = user;
            _avatar = avatar;
        }

        /**
         * Attempts to send a tell to this tab's user.
         */
        public void requestTell (final String msg)
        {
            _ctx.getChatDirector().requestTell(
                _user, msg, new ResultListener<Name>() {
                    public void requestCompleted (Name result) {
                        appendSent(msg);
                    }
                    public void requestFailed (Exception cause) {
                        // will be reported in a feedback message
                    }
                });
        }

        /**
         * Mutes this tab's user.
         */
        public void mute ()
        {
            _pane.removeTab(this);
            _ctx.getMuteDirector().setMuted(_user, true);
        }

        @Override // documentation inherited
        protected void wasAdded ()
        {
            super.wasAdded();

            // clear the alert icon, if present
            BButton btn = _pane.getTabButton(this);
            if (btn != null) {
                btn.setIcon(null);
            }
        }

        @Override // documentation inherited
        protected int[] getSpeakerAvatar (Handle speaker)
        {
            if (speaker.equals(_ctx.getUserObject().handle)) {
                PlayerObject player = _ctx.getUserObject();
                Look look = player.getLook(Look.Pose.DEFAULT);
                return (look == null) ? null : look.getAvatar(player);

            } else if (speaker.equals(_user)) {
                return _avatar;

            } else {
                // this should never happen
                log.warning("Unknown speaker [speaker=" + speaker +
                            ", user=" + _user + "].");
                return null;
            }
        }

        protected Handle _user;
        protected int[] _avatar;
    }

    protected BangContext _ctx;
    protected BTextField _input;
    protected BButton _send;
    protected TabbedPane _pane;
    protected Dimension _tabSize;

    protected HashMap<Handle,UserTab> _users =
        new HashMap<Handle,UserTab>();

    protected BIcon _alert;
}
