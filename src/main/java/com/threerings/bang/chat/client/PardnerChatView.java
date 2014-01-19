//
// $Id$

package com.threerings.bang.chat.client;

import java.util.Map;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.TellFeedbackMessage;

import com.threerings.bang.chat.data.PlayerMessage;
import com.threerings.bang.chat.client.TabbedChatView.UserTab;

import com.threerings.bang.game.data.BangObject;

import com.threerings.bang.client.BangClient;
import com.threerings.bang.client.MainView;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * A dialog through which users can exchange tells with one or more
 * of their pardners, with a display that shows the pardners' avatars
 * next to the text.
 */
public class PardnerChatView extends BDecoratedWindow
    implements ActionListener, BangCodes
{
    public PardnerChatView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), null);
        _ctx = ctx;
        setStyleClass("pardner_chat_view");
        setModal(true);
        setLayer(1);

        add(_tabView = new PardnerChatTabs(ctx));

        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        buttons.add(_mute = new BButton(ctx.xlate(BANG_MSGS, "m.chat_mute"),
            this, "mute"));
        buttons.add(_resume = new BButton(ctx.xlate(BANG_MSGS, "m.dismiss"),
                                          this, "resume"));
        add(buttons, GroupLayout.FIXED);
    }

    /**
     * Displays the chat view, if possible, with a tab for talking to the named pardner.
     *
     * @return true if we managed to display the view, false if we can't at the moment
     */
    public boolean display (Handle pardner, boolean grabFocus)
    {
        return (_tabView.openUserTab(pardner, grabFocus) != null);
    }

    /**
     * Ensure that a given user tab exists, possibly creating it.
     *
     * @param focus if true the user's tab will be made visible and the chat input field will be
     * focused. If false, the tab will be added if it does not exist but will not be made current,
     * nor will focus be moved to the input field.
     */
    public UserTab openUserTab (Handle handle, boolean focus)
    {
        return _tabView.openUserTab(handle, focus);
    }

    /**
     * Register a PlaceChatView that will display the user tabs.
     */
    public void registerPlaceChatView (PlaceChatView placeChat)
    {
        if (_placeChat != null) {
            log.warning("Cannot register, already have a PlaceChatView", "_placeChat", _placeChat);
            return;
        }

        _placeChat = placeChat;
        _tabView.giveUserTabs();
    }

    /**
     * Unregister a PlaceChatView that was displaying the user tabs.
     */
    public void unregisterPlaceChatView (PlaceChatView placeChat)
    {
        if (_placeChat != placeChat) {
            log.warning("Attempt to unregister invalid PlaceChatView", "_placeChat", _placeChat,
                        "placeChat", placeChat);
            return;
        }

        _tabView.getUserTabs();
        _placeChat = null;
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _tabView.giveUserTabs();
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        _tabView.getUserTabs();
        super.wasAdded();
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent ae)
    {
        Object src = ae.getSource();
        if (src == _mute) {
            ((UserTab)_tabView._pane.getSelectedTab()).mute();

        } else if (src == _resume) {
            _ctx.getBangClient().clearPopup(this, false);
        }
    }

    /** A subclass that knows how to display and clear the chat popup */
    protected class PardnerChatTabs extends TabbedChatView
    {
        public PardnerChatTabs (BangContext ctx)
        {
            super(ctx, new Dimension(400, 400));
        }

        @Override // from TabbedChatView
        public boolean displayMessage (ChatMessage msg, boolean alreadyDisplayed)
        {
            if (alreadyDisplayed) {
                return false;
            }

            // we handle player-to-player chat
            if (msg instanceof PlayerMessage &&
                ChatCodes.USER_CHAT_TYPE.equals(msg.localtype)) {
                PlayerMessage pmsg = (PlayerMessage)msg;
                // let players inside a game chat to each other directly
                PlaceObject plobj = _ctx.getLocationDirector().getPlaceObject();
                if (plobj instanceof BangObject && plobj.getOccupantInfo(pmsg.speaker) != null) {
                    return false;
                }
                Handle handle = (Handle)pmsg.speaker;
                UserTab tab = openUserTab(handle, false);
                if (tab == null) {
                    return false;
                }
                if (isAdded() || _placeChat == null) {
                    if (tab != _pane.getSelectedTab()) {
                        _pane.getTabButton(tab).setIcon(_alert);
                    }
                } else if (_placeChat != null) {
                    if (tab != _placeChat._pane.getSelectedTab()) {
                        _placeChat._pane.getTabButton(tab).setIcon(_alert);
                    }
                }
                tab.appendReceived(pmsg);
                return true;
            } else if (msg instanceof TellFeedbackMessage &&
                    !((TellFeedbackMessage)msg).isFailure()) {
                TellFeedbackMessage tmsg = (TellFeedbackMessage)msg;
                // let players inside a game chat to each other directly
                PlaceObject plobj = _ctx.getLocationDirector().getPlaceObject();
                if (plobj instanceof BangObject && plobj.getOccupantInfo(tmsg.speaker) != null) {
                    return false;
                }
                Handle handle = (Handle)tmsg.speaker;
                UserTab tab = openUserTab(handle, false);
                if (tab == null) {
                    return false;
                }
                tab.appendSent(tmsg.message);
                return true;
            }

            return super.displayMessage(msg, alreadyDisplayed);
        }

        /**
         * Ensure that a given user tab exists, possibly creating it.
         *
         * @param focus if true the user's tab will be made visible and the chat input field will
         * be focused. If false, the tab will be added if it does not exist but will not be made
         * current, nor will focus be moved to the input field.
         */
        public UserTab openUserTab (Handle handle, boolean focus)
        {
            UserTab tab = _users.get(handle);
            if (tab == null) {
                tab = new UserTab(_ctx, handle);
                if (isAdded() || _placeChat == null) {
                    _pane.addTab(handle.toString(), tab, true);
                    tab.setTabbedPane(_pane);
                } else {
                    _placeChat.addUserTab(handle.toString(), tab, focus);
                }
                _users.put(handle, tab);
            }

            // if we are delegating to a place chat, do that now
            if (_placeChat != null) {
                if (focus) {
                    _placeChat.showUserTab(tab);
                }

            } else if (!isAdded()) {
                if (displayTabs()) {
                    // if the interface was totally hidden, go ahead and select our tab because
                    // they obviously weren't attending to the current tab
                    _pane.selectTab(tab);
                    if (focus) {
                        _input.requestFocus();
                    }
                }

            } else if (focus) {
                _pane.selectTab(tab);
                _input.requestFocus();
            }

            return tab;
        }

        /**
         * Moves user chat tabs to a registered PlaceChatView.
         */
        public void giveUserTabs ()
        {
            if (isAdded() || _placeChat == null) {
                return;
            }
            for (Map.Entry<Handle,UserTab> entry : _users.entrySet()) {
                _pane.removeTab(entry.getValue());
                _placeChat.addUserTab(entry.getKey().toString(), entry.getValue(), false);
            }
        }

        /**
         * Retrieves user chat tabs from a registered PlaceChatView.
         */
        public void getUserTabs ()
        {
            if (_placeChat == null) {
                return;
            }
            for (Map.Entry<Handle,UserTab> entry : _users.entrySet()) {
                _placeChat.removeUserTab(entry.getValue());
                _pane.addTab(entry.getKey().toString(), entry.getValue(), false);
                entry.getValue().setTabbedPane(_pane);
            }
        }

        @Override // from TabbedChatView
        protected boolean displayTabs ()
        {
            if (isAdded()) {
                return true;
            }

            BangClient client = _ctx.getBangClient();
            if (!client.canDisplayPopup(MainView.Type.CHAT) ||
                (_placeChat != null && !client.hasPopups())) {
                return false;
            }
            _ctx.getBangClient().displayPopup(PardnerChatView.this, false);
            pack(-1, -1);
            center();
            return true;
        }

        @Override // from TabbedChatView
        protected void lastTabClosed ()
        {
            _ctx.getBangClient().clearPopup(PardnerChatView.this, false);
        }
    }

    protected BangContext _ctx;
    protected PardnerChatTabs _tabView;
    protected BButton _mute, _close, _resume;
    protected PlaceChatView _placeChat;
}
