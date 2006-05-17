//
// $Id$

package com.threerings.bang.client;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import java.util.Arrays;
import java.util.HashMap;

import java.nio.FloatBuffer;

import com.jme.math.Vector2f;
import com.jme.util.geom.BufferUtils;

import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.background.ImageBackground;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.ArrayUtil;
import com.threerings.util.Name;

import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.data.Look;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

/**
 * Displays chat with avatar icons and speech balloons.
 */
public abstract class ComicChatView extends BScrollPane
{
    public ComicChatView (BangContext ctx)
    {
        super(new BContainer(
                  GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                                       GroupLayout.STRETCH)));

        _ctx = ctx;
        _content = (BContainer)getChild();
        _vport.setStyleClass("comic_chat_viewport");
        setPreferredSize(new Dimension(400, 400));

        // render the chat bubble backgrounds
        createBubbleBackgrounds();
    }

    /**
     * Appends a message sent by the local user (only used in circumstances
     * where a player's chat is not normally echoed back to them).
     */
    public void appendSent (String msg)
    {
        appendSpoken(_ctx.getUserObject().handle, msg);
    }

    /**
     * Appends a message received from the chat director.
     */
    public void appendReceived (UserMessage msg)
    {
        appendSpoken((Handle)msg.speaker, msg.message);
    }

    protected void appendSpoken (Handle speaker, String message)
    {
        // look up our speaker record for this speaker
        Speaker sprec = _speakers.get(speaker);
        if (sprec == null) {
            sprec = new Speaker(speaker);
            _speakers.put(speaker, sprec);
        }

        // update the speaker's icon if it has changed
        boolean newav = false;
        if (sprec.setAvatar(getSpeakerAvatar(speaker))) {
            boolean mirror = isLeftSide(speaker) ^
                !_ctx.getAvatarLogic().isMale(sprec.avatar);
            sprec.icon = getAvatarIcon(sprec.avatar, mirror);
            newav = true;
        }

        // create a new chat entry if necessary
        ChatEntry entry = _last;
        if (entry == null || newav || !entry.speaker.equals(sprec)) {
            _content.add(entry = new ChatEntry(sprec, isLeftSide(speaker)));
        }

        // and append our message to that chat entry
        entry.addMessage(message);
        _scrollToEnd = true;

        // we only allow two messages to use the same avatar, then we add
        // another avatar to avoid the amazing zillion chat bubbles with no
        // head
        if (_last == null) {
            _last = entry;
        } else {
            _last = null;
        }
    }

    /**
     * Appends a message received from the system.
     */
    public void appendSystem (ChatMessage msg)
    {
        String style = SystemChatView.getAttentionLevel(msg) + "_chat_label";
        _content.add(new BLabel(SystemChatView.format(_ctx, msg), style));
        _scrollToEnd = true;
        _last = null;
    }

    /**
     * Clears out all displayed messages.
     */
    public void clear ()
    {
        _content.removeAll();
        _last = null;
    }

    @Override // documentation inherited
    public void validate ()
    {
        super.validate();

        // if flagged, scroll to the end now that everything is laid out
        if (_scrollToEnd) {
            getVerticalScrollBar().getModel().setValue(Integer.MAX_VALUE);
            _scrollToEnd = false;
        }
    }

    /**
     * Returns the avatar to use for the specified speaker.
     */
    protected abstract int[] getSpeakerAvatar (Handle speaker);

    /**
     * Returns whether this speaker should be placed on the left or right hand
     * side of the window. By default our messages are left side, everyone else
     * is right side.
     */
    protected boolean isLeftSide (Handle speaker)
    {
        return speaker.equals(_ctx.getUserObject().handle);
    }

    /**
     * Gets a scaled avatar icon for the specified avatar (which can be
     * <code>null</code>, in which case <code>null</code> is returned).
     *
     * @param mirror if true, flip the image left-to-right
     */
    protected BIcon getAvatarIcon (int[] avatar, boolean mirror)
    {
        if (avatar == null) {
            return null;
        }

        Image img = AvatarView.getImage(_ctx, avatar).getScaledInstance(
            AvatarLogic.WIDTH/8, AvatarLogic.HEIGHT/8,
            BufferedImage.SCALE_SMOOTH);
        BImage bimg;
        if (mirror) {
            bimg = new BImage(img) {
                public void setTextureCoords (
                    int sx, int sy, int swidth, int sheight) {
                    // flip the texture coords left-to-right
                    super.setTextureCoords(sx, sy, swidth, sheight);
                    FloatBuffer tcoords = getTextureBuffer();
                    swapInBuffer(tcoords, 0, 3);
                    swapInBuffer(tcoords, 1, 2);
                }
            };
        } else {
            bimg = new BImage(img);
        }
        return new ImageIcon(bimg);
    }

    /**
     * Swaps two texture coordinates in the specified buffer.
     */
    protected void swapInBuffer (FloatBuffer tbuf, int idx1, int idx2)
    {
        BufferUtils.populateFromBuffer(_tcoord, tbuf, idx1);
        BufferUtils.copyInternalVector2(tbuf, idx2, idx1);
        BufferUtils.setInBuffer(_tcoord, tbuf, idx2);
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

    /** Used to keep track of speaker's icons. */
    protected static class Speaker
    {
        public Handle handle;
        public int[] avatar;
        public BIcon icon;

        public Speaker (Handle handle) {
            this.handle = handle;
        }

        public boolean setAvatar (int[] avatar) {
            if (this.avatar != null && Arrays.equals(this.avatar, avatar)) {
                return false;
            }
            this.avatar = avatar;
            return true;
        }

        public boolean equals (Object other) {
            return handle.equals(((Speaker)other).handle);
        }
    }

    /** A chat entry that displays an avatar icon along with one or more
     * messages in bubbles. */
    protected class ChatEntry extends BContainer
    {
        /** The speaker of this entry. */
        public Speaker speaker;

        public ChatEntry (Speaker speaker, boolean left)
        {
            this.speaker = speaker;
            _left = left;

            GroupLayout layout = GroupLayout.makeHoriz(GroupLayout.STRETCH,
                GroupLayout.CENTER, GroupLayout.NONE);
            layout.setOffAxisJustification(GroupLayout.TOP);
            setLayoutManager(layout);

            layout = GroupLayout.makeVert(
                GroupLayout.NONE, GroupLayout.TOP, GroupLayout.CONSTRAIN);
            layout.setOffAxisJustification(
                _left ? GroupLayout.LEFT : GroupLayout.RIGHT);
            add(_mcont = new BContainer(layout) {
                protected Dimension computePreferredSize (
                    int whint, int hhint) {
                    return super.computePreferredSize(285, hhint);
                }
            });

            if (speaker.icon != null) {
                add(_left ? 0 : 1, new BLabel(speaker.icon), GroupLayout.FIXED);
            }
        }

        public void addMessage (String msg)
        {
            BLabel label = new BLabel(
                msg, _left ? "sent_chat_bubble" : "received_chat_bubble") {
                protected void wasAdded() {
                    super.wasAdded();
                    setBackground(
                        DEFAULT, _mcont.getComponentCount() == 1 ?
                        (_left ? _sfbg : _rfbg) : (_left ? _srbg : _rrbg));
                }
            };
            _mcont.add(label);
        }

        protected BContainer _mcont;
        protected boolean _left;
    }

    protected BangContext _ctx;
    protected BContainer _content;
    protected ChatEntry _last;
    protected boolean _scrollToEnd;

    /** Maps speakers to avatars and icons. */
    protected HashMap<Handle,Speaker> _speakers = new HashMap<Handle,Speaker>();

    /** Chat bubble backgrounds for sent and received messages, first bubble in
     * sequence and rest of bubbles in sequence. */
    protected ImageBackground _sfbg, _srbg, _rfbg, _rrbg;

    /** Used to flip texture coordinates. */
    protected Vector2f _tcoord = new Vector2f();
}
