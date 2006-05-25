//
// $Id$

package com.threerings.bang.client;

import java.util.HashMap;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.ResultListener;

import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.bang.avatar.data.Look;

import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.client.bui.TabbedPane;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * A dialog through which users can exchange tells with one or more
 * of their pardners, with a display that shows the pardners' avatars
 * next to the text.
 */
public class PardnerChatView extends BDecoratedWindow
    implements ActionListener, ChatDisplay, BangCodes
{
    public PardnerChatView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), null);
        _ctx = ctx;
        setStyleClass("pardner_chat_view");
        setModal(true);
        setLayer(1);

        ((GroupLayout)getLayoutManager()).setOffAxisPolicy(GroupLayout.STRETCH);

        _ctx.getChatDirector().addChatDisplay(this);

        add(_tabs = new TabbedPane(true));

        BContainer bottom = new BContainer(GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.CENTER, GroupLayout.STRETCH));
        BContainer tcont = new BContainer(GroupLayout.makeHoriz(
            GroupLayout.STRETCH, GroupLayout.CENTER, GroupLayout.NONE));
        tcont.add(_text = new BTextField());
        _text.addListener(this);
        tcont.add(_send = new BButton(new ImageIcon(
            _ctx.loadImage("ui/chat/bubble_icon.png")), this, "send"),
            GroupLayout.FIXED);
        bottom.add(tcont);
        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        //buttons.add(_mute = new BButton(ctx.xlate(BANG_MSGS, "m.chat_mute"),
        //    this, "mute"));
        buttons.add(_close = new BButton(ctx.xlate(BANG_MSGS,
            "m.chat_close_tab"), this, "close"));
        buttons.add(_resume = new BButton(ctx.xlate(BANG_MSGS,
            "m.status_resume"), this, "resume"));
        bottom.add(buttons);
        add(bottom, GroupLayout.FIXED);

        // disable send until some text is entered
        new EnablingValidator(_text, _send);

        _alert = new ImageIcon(_ctx.loadImage("ui/chat/alert_icon.png"));
    }

    /**
     * Displays the chat view, if possible, with a tab for talking to the
     * named pardner.
     *
     * @return true if we managed to display the view, false if we can't
     * at the moment
     */
    public boolean display (Handle handle, boolean grabFocus)
    {
        if (!_ctx.getBangClient().canDisplayPopup(MainView.Type.CHAT)) {
            return false;
        }
        if (addPardnerTab(handle) == null) {
            return false;
        }
        _ctx.getBangClient().displayPopup(this, false);
        pack(-1, -1);
        center();
        if (grabFocus) {
            _text.requestFocus();
        }
        return true;
    }

    @Override // documentation inherited
    public void wasRemoved ()
    {
        super.wasRemoved();
        clear();
    }

    // documentation inherited from interface ChatDisplay
    public void clear ()
    {
        _tabs.removeAllTabs();
        _pardners.clear();
        _text.setText("");
    }

    // documentation inherited from interface ChatDisplay
    public void displayMessage (ChatMessage msg)
    {
        // we handle player-to-player chat
        if (msg instanceof UserMessage &&
            ChatCodes.USER_CHAT_TYPE.equals(msg.localtype)) {
            UserMessage umsg = (UserMessage)msg;
            if (!isAdded() && !display((Handle)umsg.speaker, false)) {
                return;
            }
            PardnerTab tab = _pardners.get(umsg.speaker);
            if (tab == null) {
                tab = addPardnerTab((Handle)umsg.speaker);
            }
            if (tab != null) {
                if (tab != _tabs.getSelectedTab()) {
                    _tabs.getTabButton(tab).setIcon(_alert);
                }
                tab.appendReceived(umsg);
            }

// TODO: right now this shows up in the main UI which is weird but we need to
// differentiate between feedback as a result of our tell and general chat
// feedback messages to do the right thing...
//         } else if (msg instanceof SystemMessage &&
//                    ((SystemMessage)msg).attentionLevel ==
//                    SystemMessage.FEEDBACK) {
//             // we also have to handle feedback messages because that's how tell
//             // failures are reported
//             ((PardnerTab)_tabs.getSelectedTab()).appendSystem(msg, "feedback");
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent ae)
    {
        Object src = ae.getSource();
        if (src == _send || (src == _text && _send.isEnabled())) {
            String msg = _text.getText().trim();
            _text.setText("");
            if (msg.startsWith("/")) {
                String error = _ctx.getChatDirector().requestChat(
                    null, msg, true);
                if (!ChatCodes.SUCCESS.equals(error)) {
                    SystemMessage sysmsg = new SystemMessage(
                        _ctx.xlate(CHAT_MSGS, error), null,
                        SystemMessage.FEEDBACK);
                    ((PardnerTab)_tabs.getSelectedTab()).appendSystem(sysmsg);
                }

            } else {
                ((PardnerTab)_tabs.getSelectedTab()).requestTell(msg);
            }

        } else if (src == _mute) {
            ((PardnerTab)_tabs.getSelectedTab()).mute();

        } else if (src == _close) {
            ((PardnerTab)_tabs.getSelectedTab()).close();

        } else if (src == _resume) {
            _ctx.getBangClient().clearPopup(this, false);
        }
    }

    /**
     * Creates, adds, and maps a tab for the named pardner.
     */
    protected PardnerTab addPardnerTab (Handle handle)
    {
        PardnerEntry entry = _ctx.getUserObject().pardners.get(handle);
        if (entry == null) {
            return null;
        }
        PardnerTab tab = new PardnerTab(_ctx, entry);
        _tabs.addTab(handle.toString(), tab);
        _pardners.put(handle, tab);
        return tab;
    }

    /**
     * Handles the chat display for single pardner.
     */
    protected class PardnerTab extends ComicChatView
    {
        public PardnerTab (BangContext ctx, PardnerEntry pardner)
        {
            super(ctx, false);
            _pardner = pardner;
        }

        /**
         * Attempts to send a tell to this tab's pardner.
         */
        public void requestTell (final String msg)
        {
            _ctx.getChatDirector().requestTell(
                _pardner.handle, msg, new ResultListener() {
                    public void requestCompleted (Object result) {
                        appendSent(msg);
                    }
                    public void requestFailed (Exception cause) {
                        // will be reported in a feedback message
                    }
                });
        }

        /**
         * Mutes this tab's pardner.
         */
        public void mute ()
        {
            close();
            _ctx.getMuteDirector().setMuted(_pardner.handle, true);
        }

        /**
         * Closes this tab and hides the pop-up if it was the last tab open.
         */
        public void close ()
        {
            _tabs.removeTab(this);
            _pardners.remove(_pardner.handle);
            if (_tabs.getTabCount() == 0) {
                _ctx.getBangClient().clearPopup(PardnerChatView.this, false);
            }
        }

        @Override // documentation inherited
        protected void wasAdded ()
        {
            super.wasAdded();

            // clear the alert icon, if present
            BButton btn = _tabs.getTabButton(this);
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

            } else if (speaker.equals(_pardner.handle)) {
                return _pardner.avatar;

            } else {
                // this should never happen
                log.warning("Unknown speaker [speaker=" + speaker +
                            ", pardner=" + _pardner.handle + "].");
                return null;
            }
        }

        protected PardnerEntry _pardner;
    }

    protected BangContext _ctx;
    protected TabbedPane _tabs;
    protected BTextField _text;
    protected BButton _send, _mute, _close, _resume;

    protected HashMap<Handle,PardnerTab> _pardners =
        new HashMap<Handle,PardnerTab>();

    protected BIcon _alert;
}
