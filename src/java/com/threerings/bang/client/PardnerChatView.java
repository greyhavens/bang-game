//
// $Id$

package com.threerings.bang.client;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import java.util.Arrays;
import java.util.HashMap;

import com.jme.renderer.Renderer;
import com.jme.util.TextureManager;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BTabbedPane;
import com.jmex.bui.BTextArea;
import com.jmex.bui.BTextField;
import com.jmex.bui.background.BBackground;
import com.jmex.bui.background.ImageBackground;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.event.TextListener;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Insets;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

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
    implements ActionListener, TextListener, ChatDisplay, BangCodes
{
    public PardnerChatView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), null);
        _ctx = ctx;
        setStyleClass("pardner_chat_view");
        setModal(true);

        ((GroupLayout)getLayoutManager()).setOffAxisPolicy(GroupLayout.STRETCH);

        _ctx.getChatDirector().addChatDisplay(this);

        // shrink the tab background so that the tabs overlap the top edge and
        // the scroll bar falls outside
        _tabs = new BTabbedPane() {
            protected void renderBackground (Renderer renderer) {
                BBackground background = getBackground();
                if (background != null) {
                    background.render(renderer, 0, 0, _width - 18,
                        _height - 30);
                }
            }
        };
        add(_tabs);

        BContainer bottom = new BContainer(GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.CENTER, GroupLayout.STRETCH));
        BContainer tcont = new BContainer(GroupLayout.makeHoriz(
            GroupLayout.STRETCH, GroupLayout.CENTER, GroupLayout.NONE));
        tcont.add(_text = new BTextField());
        _text.addListener(this);
        tcont.add(_send = new BButton(new ImageIcon(
            _ctx.loadImage("ui/chat/bubble_icon.png")), this, "send"),
            GroupLayout.FIXED);
        _send.setEnabled(false);
        bottom.add(tcont);
        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        buttons.add(_mute = new BButton(ctx.xlate(BANG_MSGS, "m.chat_mute"),
            this, "mute"));
        buttons.add(_close = new BButton(ctx.xlate(BANG_MSGS,
            "m.chat_close_tab"), this, "close"));
        buttons.add(_resume = new BButton(ctx.xlate(BANG_MSGS,
            "m.status_resume"), this, "resume"));
        bottom.add(buttons);
        add(bottom, GroupLayout.FIXED);

        _alert = new ImageIcon(_ctx.loadImage("ui/chat/alert_icon.png"));
        
        // render the chat bubble backgrounds
        createBubbleBackgrounds();
    }

    /**
     * Displays the chat view, if possible, with a tab for talking to the
     * named pardner.
     *
     * @return true if we managed to display the view, false if we can't
     * at the moment
     */
    public boolean display (Name handle)
    {
        if (!_ctx.getBangClient().canDisplayPopup(MainView.Type.CHAT)) {
            return false;
        }
        addPardnerTab(handle);
        _ctx.getBangClient().displayPopup(this, false);
        pack(-1, -1);
        center();
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
        _send.setEnabled(false);
    }

    // documentation inherited from interface ChatDisplay
    public void displayMessage (ChatMessage msg)
    {
        if (msg instanceof UserMessage) {
            UserMessage umsg = (UserMessage)msg;
            if (!isAdded() && !display(umsg.speaker)) {
                return;
            }
            PardnerTab tab = _pardners.get(umsg.speaker);
            if (tab == null) {
                tab = addPardnerTab(umsg.speaker); 
            }
            if (tab != _tabs.getSelectedTab()) {
                _tabs.getTabButton(tab).setIcon(_alert);
            }
            tab.appendReceived(umsg);

        } else if (msg instanceof SystemMessage && isAdded()) {
            SystemMessage smsg = (SystemMessage)msg;
            ((PardnerTab)_tabs.getSelectedTab()).appendSystem(smsg);
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
                String error = _ctx.getChatDirector().requestChat(null, msg,
                    true);
                if (!ChatCodes.SUCCESS.equals(error)) {
                    _ctx.getChatDirector().displayFeedback(CHAT_MSGS, error);
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

    // documentation inherited from interface TextListener
    public void textChanged (TextEvent te)
    {
        _send.setEnabled(!StringUtil.isBlank(_text.getText()));
    }

    /**
     * Renders the chat bubble backgrounds.
     */
    protected void createBubbleBackgrounds ()
    {
        BufferedImage img = new BufferedImage(90, 45,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D gfx = img.createGraphics();
        Area bubble = new Area(new RoundRectangle2D.Float(8, 0, 81, 44, 30,
            30));
        bubble.add(new Area(new Arc2D.Float(-12, -8, 24, 24, -40f, 30f,
            Arc2D.PIE)));
        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        gfx.setColor(new Color(0xF1EFE3));
        gfx.fill(bubble);
        gfx.setColor(new Color(0x896A4B));
        gfx.draw(bubble);

        // flip image up and down for first and rest of sent
        _sfbg = new ImageBackground(
            ImageBackground.FRAME_XY, new BImage(img, false));
        _srbg = new ImageBackground(
            ImageBackground.FRAME_XY, new BImage(img, true));

        // flip left-to-right for received
        mirrorImage(img);
        _rfbg = new ImageBackground(
            ImageBackground.FRAME_XY, new BImage(img, false));
        _rrbg = new ImageBackground(
            ImageBackground.FRAME_XY, new BImage(img, true));
    }

    /**
     * Flips the given {@link BufferedImage} left-to-right.
     */
    protected void mirrorImage (BufferedImage img)
    {
        int w = img.getWidth();
        int[] pbuf = new int[w];
        for (int y = 0, h = img.getHeight(); y < h; y++) {
            img.getRGB(0, y, w, 1, pbuf, 0, w);
            ArrayUtil.reverse(pbuf);
            img.setRGB(0, y, w, 1, pbuf, 0, w);
        }
    }

    /**
     * Gets a scaled avatar icon for the specified avatar (which can be
     * <code>null</code>, in which case <code>null</code> is returned).
     */
    protected BIcon getAvatarIcon (int[] avatar)
    {
        return (avatar == null) ? null : new ImageIcon(
            new BImage(
                AvatarView.getImage(_ctx, avatar).getScaledInstance(
                    AVATAR_WIDTH, AVATAR_HEIGHT, BufferedImage.SCALE_SMOOTH)));
    }

    /**
     * Creates, adds, and maps a tab for the named pardner.
     */
    protected PardnerTab addPardnerTab (Name handle)
    {
        PardnerTab tab = new PardnerTab(handle);
        _tabs.addTab(handle.toString(), tab);
        _pardners.put(handle, tab);
        return tab;
    }
    
    /**
     * Handles the display for single pardner.
     */
    protected class PardnerTab extends BScrollPane
    {
        public PardnerTab (Name handle)
        {
            super(new BContainer(GroupLayout.makeVert(
                GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH)));
            _content = (BContainer)getChild();
            _handle = handle;

            _vport.setStyleClass("pardner_chat_viewport");
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
            if (_last == null || !_last.sent) {
                _content.add(_last = new ChatEntry(_micon, true));
            }
            _last.addMessage(msg);
            _scrollToEnd = true;
        }

        /**
         * Appends a message received from the pardner.
         */
        public void appendReceived (UserMessage msg)
        {
            PardnerEntry entry =
                (PardnerEntry)_ctx.getUserObject().pardners.get(_handle);
            int[] avatar = (entry == null) ? null : entry.avatar;
            boolean newav = false;
            if (!Arrays.equals(avatar, _pavatar)) {
                _picon = getAvatarIcon(avatar);
                _pavatar = avatar;
                newav = true;
            }
            if (newav || _last == null || _last.sent) {
                _content.add(_last = new ChatEntry(_picon, false));
            }
            _last.addMessage(msg.message);
            _scrollToEnd = true;
        }

        /**
         * Appends a message received from the system.
         */
        public void appendSystem (SystemMessage msg)
        {
            _last = null;
            BTextArea area = new BTextArea(msg.message);
            area.setStyleClass("system_chat_entry");
            _content.add(area);
            _scrollToEnd = true;
        }

        /**
         * Mutes the user behind this tab.
         */
        public void mute ()
        {
            close();
            _ctx.getMuteDirector().setMuted(_handle, true);   
        }
        
        /**
         * Closes this tab and hides the pop-up if this was the last tab open.
         */
        public void close ()
        {
            if (_tabs.getTabCount() == 1) {
                _ctx.getBangClient().clearPopup(PardnerChatView.this, false);
                
            } else {
                _tabs.removeTab(this);
                _pardners.remove(_handle);
            }
        }
        
        @Override // documentation inherited
        public void validate ()
        {
            // if flagged, scroll to the end when everything is valid
            super.validate();
            if (_scrollToEnd) {
                getVerticalScrollBar().getModel().setValue(Integer.MAX_VALUE);
                _scrollToEnd = false;
            }
        }
        
        @Override // documentation inherited
        protected void wasAdded ()
        {
            super.wasAdded();
            
            // clear the alert icon, if present
            _tabs.getTabButton(this).setIcon(null);
        }

        protected Name _handle;
        protected BContainer _content;
        protected ChatEntry _last;
        protected boolean _scrollToEnd;
        
        protected BIcon _picon;
        protected int[] _pavatar;
    }

    /** A chat entry that displays an avatar icon along with one or more
     * messages in bubbles. */
    protected class ChatEntry extends BContainer
    {
        /** Whether or not this entry was sent from the local player. */
        public boolean sent;

        public ChatEntry (BIcon icon, boolean sent)
        {
            this.sent = sent;

            GroupLayout layout = GroupLayout.makeHoriz(GroupLayout.STRETCH,
                GroupLayout.CENTER, GroupLayout.NONE);
            layout.setOffAxisJustification(GroupLayout.TOP);
            setLayoutManager(layout);

            layout = GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                GroupLayout.CONSTRAIN);
            layout.setOffAxisJustification(sent ?
                GroupLayout.LEFT : GroupLayout.RIGHT);
            add(_mcont = new BContainer(layout));

            if (icon != null) {
                add(sent ? 0 : 1, new BLabel(icon), GroupLayout.FIXED);
            }
        }

        public void addMessage (String msg)
        {
            BTextArea area = new BTextArea(msg);
            area.setStyleClass(sent ?
                "sent_chat_bubble" : "received_chat_bubble");
            _mcont.add(area);
            area.setBackground(BComponent.DEFAULT,
                _mcont.getComponentCount() == 1 ?
                    (sent ? _sfbg : _rfbg) : (sent ? _srbg : _rrbg));
        }

        protected BContainer _mcont;
    }

    protected BangContext _ctx;

    protected BTabbedPane _tabs;
    protected BTextField _text;
    protected BButton _send, _mute, _close, _resume;

    protected HashMap<Name, PardnerTab> _pardners =
        new HashMap<Name, PardnerTab>();

    /** Chat bubble backgrounds for sent and received messages, first bubble
     * in sequence and rest of bubbles in sequence. */
    protected ImageBackground _sfbg, _srbg, _rfbg, _rrbg;

    protected BIcon _micon, _alert;
    protected int[] _mavatar;

    /** The dimensions of the avatars in the chat window. */
    protected static final int AVATAR_WIDTH = 58, AVATAR_HEIGHT = 75;
}
