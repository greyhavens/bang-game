//
// $Id$

package com.threerings.bang.client;

import java.awt.Image;

import java.util.Arrays;
import java.util.HashMap;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BTabbedPane;
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
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.data.Look;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

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
        setModal(true);
        
        _ctx.getChatDirector().addChatDisplay(this);
        
        add(_tabs = new BTabbedPane());
        
        BContainer bottom = new BContainer(GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.CENTER, GroupLayout.STRETCH));
        bottom.add(_text = new BTextField());
        _text.addListener(this);
        BContainer buttons = GroupLayout.makeHBox(GroupLayout.RIGHT);
        buttons.add(_close = new BButton(ctx.xlate(BANG_MSGS,
            "m.chat_close")));
        _close.addListener(this);
        bottom.add(buttons);
        add(bottom, GroupLayout.FIXED);
    }
    
    /**
     * Displays a tab for talking to the named pardner.  If the chat view isn't
     * visible, show it (if possible).  If the tab doesn't exist, create it.
     * If it's not selected, select it.
     *
     * @return true if we managed to display the tab, false if we can't show
     * the chat view at the moment
     */
    public boolean displayTab (Name handle)
    {
        if (!isAdded() &&
            !_ctx.getBangClient().canDisplayPopup(MainView.Type.CHAT)) {
            return false;
        } 
        PardnerTab tab = _pardners.get(handle);
        if (tab == null) {
            _tabs.addTab(handle.toString(),
                tab = new PardnerTab(handle, _tabs.getTabCount()));
            _pardners.put(handle, tab);
        }
        _tabs.selectTab(tab.idx);
        if (!isAdded()) {
            _ctx.getBangClient().displayPopup(this, false);
            pack(-1, -1);
            center();
        }
        return true;
    }
    
    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();
        
        // update the avatar icon
        PlayerObject player = _ctx.getUserObject();
        Look look = (Look)player.looks.get(player.look);
        int[] avatar = (look == null) ? null : look.getAvatar(player);
        if (!Arrays.equals(_mavatar, avatar)) {
            _micon = getAvatarIcon(avatar);
            _mavatar = avatar;
        }
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
        if (msg instanceof UserMessage) {
            UserMessage umsg = (UserMessage)msg;
            if (displayTab(umsg.speaker)) {
                _pardners.get(umsg.speaker).appendReceived(umsg);
            }
    
        } else if (msg instanceof SystemMessage && isAdded()) {
            SystemMessage smsg = (SystemMessage)msg;
            ((PardnerTab)_tabs.getSelectedTab()).appendSystem(smsg);
        }
    }
    
    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent ae)
    {
        Object src = ae.getSource();
        if (src == _text) {
            String msg = _text.getText().trim();
            _text.setText("");
            if (msg.startsWith("/")) {
                String error = _ctx.getChatDirector().requestChat(null, msg,
                    true);
                if (!ChatCodes.SUCCESS.equals(error)) {
                    _ctx.getChatDirector().displayFeedback(CHAT_MSGS, error);
                }
                
            } else {
                ((PardnerTab)_tabs.getSelectedTab()).requestTell(msg);
            }
            
        } else if (src == _close) {
            _ctx.getBangClient().clearPopup(this, false);
        }
    }

    /**
     * Gets a scaled avatar icon for the specified avatar (which can be
     * <code>null</code>, in which case <code>null</code> is returned).
     */
    protected BIcon getAvatarIcon (int[] avatar)
    {
        return (avatar == null) ? null : new ImageIcon(
            AvatarView.getImage(_ctx, avatar).getScaledInstance(
                AVATAR_WIDTH, AVATAR_HEIGHT, Image.SCALE_SMOOTH));
    }
    
    /**
     * Handles the display for single pardner.
     */
    protected class PardnerTab extends BScrollPane
    {
        /** The index of the tab. */
        public int idx;
        
        public PardnerTab (Name handle, int idx)
        {
            super(new BContainer(GroupLayout.makeVert(
                GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH)));
            _content = (BContainer)getChild();
            _handle = handle;
            this.idx = idx;
            
            setPreferredSize(new Dimension(400, 400));
        }
        
        /**
         * Attempts to send a tell to this tab's pardner.
         */
        public void requestTell (final String msg)
        {
            _ctx.getChatDirector().requestTell(_handle, msg,
                new ResultListener() {
                    public void requestCompleted (Object result) {
                        appendSent(msg);
                    }
                    public void requestFailed (Exception cause) {
                        // will be reported in a feedback message
                    }
                });
        }
        
        /**
         * Appends a message sent by the local user.
         */
        public void appendSent (String msg)
        {
            append(new ChatBubble(_micon, msg, true));
        }

        /**
         * Appends a message received from the pardner.
         */
        public void appendReceived (UserMessage msg)
        {
            PardnerEntry entry =
                (PardnerEntry)_ctx.getUserObject().pardners.get(_handle);
            int[] avatar = (entry == null) ? null : entry.avatar;
            if (!Arrays.equals(avatar, _pavatar)) {
                _picon = getAvatarIcon(avatar);
                _pavatar = avatar;
            }
            append(new ChatBubble(_picon, msg.message, false));
        }
        
        /**
         * Appends a message received from the system.
         */ 
        public void appendSystem (SystemMessage msg)
        {
            append(new BLabel(_ctx.xlate(msg.bundle, msg.message)));
        }
        
        protected void append (BComponent comp)
        {
            // add the new component, validate the viewport to make sure the scroll
            // bounds are up-to-date, and scroll to visible
            _content.add(comp);
            _vport.validate();
            getVerticalScrollBar().getModel().setValue(Integer.MAX_VALUE);
        }
        
        protected Name _handle;
        protected BContainer _content;
        
        protected BIcon _picon;
        protected int[] _pavatar;
    }
    
    /** A chat bubble that displays an avatar icon along with a message. */
    protected class ChatBubble extends BContainer
    {
        public ChatBubble (BIcon icon, String msg, boolean self)
        {
            super(GroupLayout.makeHoriz(GroupLayout.STRETCH,
                self ? GroupLayout.LEFT : GroupLayout.RIGHT,
                GroupLayout.NONE));
            add(new BLabel(msg));
            if (icon != null) {
                add(self ? 0 : 1, new BLabel(icon), GroupLayout.FIXED);
            }
        }
    }
    
    protected BangContext _ctx;
    
    protected BTabbedPane _tabs;
    protected BTextField _text;
    protected BButton _close;
    
    protected HashMap<Name, PardnerTab> _pardners =
        new HashMap<Name, PardnerTab>();
    
    protected BIcon _micon;
    protected int[] _mavatar;
    
    protected static final int AVATAR_WIDTH = 58; 
    
    protected static final int AVATAR_HEIGHT = 75;
}
