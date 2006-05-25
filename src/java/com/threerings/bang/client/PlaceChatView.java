//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

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

/**
 * A chat view implementation that uses avatars and balloons but speaks on the
 * default PlaceObject chat channel.
 */
public class PlaceChatView extends ComicChatView
    implements ChatDisplay, ActionListener
{
    public PlaceChatView (BangContext ctx)
    {
        super(ctx, true);
        _vport.setStyleClass("place_chat_viewport");

        BContainer tcont = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.STRETCH, GroupLayout.CENTER,
                                  GroupLayout.NONE));
        tcont.add(new Spacer(5, 5), GroupLayout.FIXED);
        tcont.add(_text = new BTextField());
        _text.addListener(this);
        ImageIcon icon = new ImageIcon(
            _ctx.loadImage("ui/chat/bubble_icon.png"));
        tcont.add(_send = new BButton(icon, this, "send"), GroupLayout.FIXED);
        add(tcont, BorderLayout.SOUTH);
        
        // disable send until some text is entered
        new EnablingValidator(_text, _send);
    }

    /**
     * This should be called on the place chat view when we have access to our
     * place object.
     */
    public void willEnterPlace (PlaceObject plobj)
    {
        setSpeakService(plobj.speakService);
    }

    /**
     * This should be called when we have left our place.
     */
    public void didLeavePlace (PlaceObject plobj)
    {
        clearSpeakService();
    }

    /**
     * A place chat view can be used on a custom speak service by calling this
     * method instead of calling {@link #willEnterPlace}. It should be paired
     * with a call to {@link #clearSpeakService}.
     *
     * <p><em>Note:</em> in that case, {@link #getSpeakerAvatar} will likely
     * need to be overridden as the default implementation obtains avatars from
     * the occupant director.
     */
    public void setSpeakService (SpeakService spsvc)
    {
        if (spsvc != null) {
            _spsvc = spsvc;
            _ctx.getChatDirector().addChatDisplay(this);
        }
    }

    /**
     * Clears a custom speak service set earlier with a call to {@link
     * #setSpeakService}.
     */
    public void clearSpeakService ()
    {
        if (_spsvc != null) {
            _ctx.getChatDirector().removeChatDisplay(this);
            _spsvc = null;
        }
    }

    /**
     * Displays an informational message specific to the place.
     */
    public void displayInfo (String bundle, String msg)
    {
        SystemMessage smsg = new SystemMessage(msg, bundle,
            SystemMessage.INFO);
        smsg.setClientInfo(_ctx.xlate(bundle, msg), ChatCodes.PLACE_CHAT_TYPE);
        appendSystem(smsg);
    }
    
    // documentation inherited from interface ChatDisplay
    public void displayMessage (ChatMessage msg)
    {
        if (msg instanceof UserMessage) {
            UserMessage umsg = (UserMessage)msg;
            if (umsg.mode == ChatCodes.BROADCAST_MODE) {
                // we don't handle broadcast messages
            } else if (umsg.localtype.equals(ChatCodes.USER_CHAT_TYPE)) {
                // we don't handle user-to-user chat, let the pardner chat view
                // deal with that
            } else {
                appendReceived(umsg);
            }
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent ae)
    {
        Object src = ae.getSource();
        if (src == _send || (src == _text && _send.isEnabled())) {
            String msg = _text.getText().trim();
            _text.setText("");
            String error = _ctx.getChatDirector().requestChat(null, msg, true);
            if (!ChatCodes.SUCCESS.equals(error)) {
                SystemMessage sysmsg = new SystemMessage(
                    _ctx.xlate(BangCodes.CHAT_MSGS, error), null,
                    SystemMessage.FEEDBACK);
                appendSystem(sysmsg);
            }
        }
    }

    @Override // documentation inherited
    protected int[] getSpeakerAvatar (Handle speaker)
    {
        BangOccupantInfo boi = (BangOccupantInfo)
            _ctx.getOccupantDirector().getOccupantInfo(speaker);
        return boi == null ? null : boi.avatar;
    }

    protected SpeakService _spsvc;
    protected BTextField _text;
    protected BButton _send;
}
