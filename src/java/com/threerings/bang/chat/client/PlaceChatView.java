//
// $Id$

package com.threerings.bang.chat.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.client.SpeakService;
import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;
import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangOccupantInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Extends the {@link TabbedChatView} and adds a tab for speaking on the
 * current PlaceObject chat channel.
 */
public class PlaceChatView extends TabbedChatView
{
    /** A chat "localtype" for messages generated on the client that should
     * be shown in the place chat view as opposed to the
     * {@link SystemChatView}. */
    public static final String PLACE_CHAT_VIEW_TYPE = "placeChatView";

    public PlaceChatView (BangContext ctx, String title)
    {
        super(ctx, new Dimension(400, 400));

        _pchat = new ComicChatView(ctx, _tabSize, true) {
            protected int[] getSpeakerAvatar (Handle speaker) {
                return PlaceChatView.this.getSpeakerAvatar(speaker);
            }
        };
        _pane.addTab(title, _pchat);
    }

    /**
     * A place chat view can be used on a custom speak service by calling this
     * method. It should be paired with a call to {@link #clearSpeakService}.
     *
     * <p><em>Note:</em> in that case, {@link #getSpeakerAvatar} will likely
     * need to be overridden as the default implementation obtains avatars from
     * the occupant director.
     */
    public void setSpeakService (SpeakService spsvc)
    {
        if (spsvc != null) {
            _spsvc = spsvc;
        }
    }

    /**
     * Clears a custom speak service set earlier with a call to {@link
     * #setSpeakService}.
     */
    public void clearSpeakService ()
    {
        if (_spsvc != null) {
            _spsvc = null;
        }
    }

    /**
     * Displays an informational message in the place chat view.
     */
    public void displayInfo (String bundle, String msg)
    {
        _ctx.getChatDirector().displayInfo(bundle, msg, PLACE_CHAT_VIEW_TYPE);
    }

    /**
     * Adds a usertab to the pane.
     */
    public void addUserTab (String handle, UserTab tab, boolean focus)
    {
        _pane.addTab(handle, tab, true);
        tab.setTabbedPane(_pane);

        if (focus) {
            _pane.selectTab(tab);
            _input.requestFocus();
        }
    }

    /**
     * Removes a usertab from the pane.
     */
    public void removeUserTab (UserTab tab)
    {
        _pane.removeTab(tab);
    }

    // documentation inherited from interface ChatDisplay
    public boolean displayMessage (ChatMessage msg, boolean alreadyDisplayed)
    {
        if (alreadyDisplayed) {
            return false;
        }

        // if it's not place chat, pass it to our parent
        if (!msg.localtype.equals(PLACE_CHAT_VIEW_TYPE) &&
            !msg.localtype.equals(ChatCodes.PLACE_CHAT_TYPE)) {
            return super.displayMessage(msg, alreadyDisplayed);
        }

        if (msg instanceof UserMessage) {
            UserMessage umsg = (UserMessage)msg;
            if (umsg.mode == ChatCodes.BROADCAST_MODE) {
                return false; // we don't handle broadcast messages
            } else {
                _pchat.appendReceived(umsg);
                return true;
            }

        } else if (msg instanceof SystemMessage) {
            SystemMessage smsg = (SystemMessage)msg;
            if (PLACE_CHAT_VIEW_TYPE.equals(msg.localtype) ||
                SystemMessage.FEEDBACK == smsg.attentionLevel) {
                _pchat.appendSystem(msg);
                return true;
            }
        }

        return false;
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent ae)
    {
        // if the place chat tab is not selected, let our parent handle it
        if (_pane.getSelectedTab() != _pchat) {
            super.actionPerformed(ae);
            return;
        }

        // make sure this came from a known widget
        Object src = ae.getSource();
        if (src != _send && !(src == _input && _send.isEnabled())) {
            return;
        }

        // request to chat
        String msg = _input.getText().trim();
        _input.setText("");
        String error = _ctx.getChatDirector().requestChat(_spsvc, msg, true);
        if (!ChatCodes.SUCCESS.equals(error)) {
            error = _ctx.xlate(BangCodes.CHAT_MSGS, error);
            SystemMessage sysmsg = new SystemMessage(
                error, null, SystemMessage.FEEDBACK);
            _pchat.appendSystem(sysmsg);
        }
    }

    /**
     * Returns the avatar representing the specified speaker.
     */
    protected int[] getSpeakerAvatar (Handle speaker)
    {
        BangOccupantInfo boi =
            (BangOccupantInfo)_ctx.getOccupantDirector().getOccupantInfo(speaker);
        return (boi == null) ? null : boi.avatar;
    }
    
    @Override // documentation inherited
    protected void wasAdded ()
    {
        _ctx.getBangClient().getPardnerChatView().registerPlaceChatView(this);
        super.wasAdded();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // save halted message for the game
        ((BangChatDirector)_ctx.getChatDirector()).setHaltedMessage(
            _input.getText());
        _ctx.getBangClient().getPardnerChatView().unregisterPlaceChatView(this);
    }

    protected SpeakService _spsvc;
    protected ComicChatView _pchat;
}
