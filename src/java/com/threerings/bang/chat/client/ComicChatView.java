//
// $Id$

package com.threerings.bang.chat.client;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.HashMap;

import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollingList;
import com.jmex.bui.background.ImageBackground;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.icon.BIcon;
import com.jmex.bui.icon.BlankIcon;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.bang.avatar.client.AvatarView;
import com.threerings.bang.avatar.util.AvatarLogic;

import com.threerings.bang.client.PlayerPopupMenu;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;

// this seems strange but we need it so that we can use our own inner class to
// parameterize the type of our parent class
import com.threerings.bang.chat.client.ComicChatView.EntryBuilder;

/**
 * Displays chat with avatar icons and speech balloons.
 */
public abstract class ComicChatView
    extends BScrollingList<EntryBuilder<BComponent>, BComponent>
    implements ChatTab
{
    /**
     *  A factory of entries to send into {@link BScrollingList}.
     */
    public interface EntryBuilder<T extends BComponent>
    {
        /**
         *  Construct and return whatever it is we were meant to construct.
         */
        public T build();
    }

    public ComicChatView (BangContext ctx, Dimension size, boolean showNames)
    {
        super();

        _ctx = ctx;
        _showNames = showNames;
        _vport.setStyleClass("comic_chat_viewport");
        // this class does not function well without an absolute size
        setPreferredSize(size);

        // render the chat bubble backgrounds
        createBubbleBackgrounds();
    }

    // documentation inherited from interface ChatTab
    public void appendSent (String msg)
    {
        String filtered = _ctx.getChatDirector().filter(msg, null, true);
        if (!StringUtil.isBlank(filtered)) {
            appendSpoken(_ctx.getUserObject().handle, filtered);
        }
    }

    // documentation inherited from interface ChatTab
    public void appendReceived (UserMessage msg)
    {
        appendSpoken((Handle)msg.speaker, msg.message);
    }

    @Override // from BScrollingList
    protected BComponent createComponent (EntryBuilder<BComponent> builder)
    {
        return builder.build();
    }

    /**
     *  A concrete {@link EntryBuilder} that lazily constructs a fancy {@link ChatEntry} instance
     *  on demand.
     *
     *  Additional complexity results from the need to temporarily cache a version of the generated
     *  {@link ChatEntry} so that a second chat message may be appended to it.
     */
    protected class ChatEntryBuilder implements EntryBuilder<BComponent>
    {
        /**
         *  Construct a {@link ChatEntry} or read it from cache, then return it.
         */
        public ChatEntry build ()
        {
            // if we have a cached one, use it
            if (_cachedEntry != null) {
                return _cachedEntry;
            }

            // look up our speaker record for this speaker
            Speaker sprec = _speakers.get(_speaker);

            sprec.loadAvatar(_ctx,
                    isLeftSide(_speaker) ^ !_ctx.getAvatarLogic().isMale(sprec.getAvatar()));

            ChatEntry entry = new ChatEntry(sprec, isLeftSide(_speaker));
            if (_message != null) {
                entry.addMessage(_message);
            }
            if (_secondMessage != null) {
                entry.addMessage(_secondMessage);
            }
            return entry;
        }

        /**
         *  Construct a new builder, optionally constructing and caching a {@link ChatEntry}
         *  immediately.
         */
        protected ChatEntryBuilder (Handle speaker, String message, boolean cache)
        {
            _speaker = speaker;
            _message = message;

            // look up our speaker record for this speaker
            Speaker sprec = _speakers.get(_speaker);
            if (sprec == null) {
                sprec = new Speaker(_speaker);
                _speakers.put(_speaker, sprec);
            }

            // update the speaker's avatar in case it has changed
            AvatarInfo avatar = getSpeakerAvatar(_speaker);
            sprec.setAvatar(avatar);

            if (cache) {
                _cachedEntry = build();
            } else {
                _cachedEntry = null;
            }
        }

        /**
         *  Two messages in a row from the same user share the same {@link ChatEntry}; this method
         *  stores the second message.
         */
        protected void setSecondMessage (String message)
        {
            _secondMessage = message;
            if (_cachedEntry != null) {
                _cachedEntry.addMessage(_secondMessage);
            }
        }

        /** Just export this entry builder's speaker */
        protected Handle getSpeaker ()
        {
            return _speaker;
        }

        /**
         * When we're done adding messages to this entry, we absolutely must get rid of the cached
         * entry, or the whole point is lost.
         */
        protected void clearCachedEntry ()
        {
            _cachedEntry = null;
        }

        protected ChatEntry _cachedEntry;
        protected Handle _speaker;
        protected String _message, _secondMessage;
    }

    protected void appendSpoken (Handle speaker, String message)
    {
        ChatEntryBuilder builder = _last;
        if (builder == null || !builder.getSpeaker().equals(speaker)) {
            builder = new ChatEntryBuilder(speaker, message, true);
            addValue(builder, true);
            if (_values.size() > MAX_VALUES) {
                removeValuesFromTop(REMOVE_SIZE);
            }
        } else {
            builder.setSecondMessage(message);
            // clear out the cached height for our builder or the scrolling list won't properly
            // figure things out
            _values.get(_values.size()-1).height = -1;
            // and inject a snap to bottom request
            _vport.invalidateAndSnap();
        }

        if (_last != builder) {
            _last = builder;
        } else {
            if (_last != null) {
                _last.clearCachedEntry();
            }
            _last = null;
        }
    }

    // documentation inherited from interface ChatTab
    public void appendSystem (ChatMessage msg)
    {
        final String formattedMsg = SystemChatView.format(_ctx, msg);
        final String style = SystemChatView.getAttentionLevel(msg) + "_chat_label";
        addValue(new EntryBuilder<BComponent>() {
            public BComponent build() {
                return new BLabel(formattedMsg, style);
            }
        }, true);
        _last = null;
    }

    // documentation inherited from interface ChatTab
    public void clear ()
    {
        removeValues();
        _last = null;
    }

    /**
     * Returns the avatar to use for the specified speaker.
     */
    protected abstract AvatarInfo getSpeakerAvatar (Handle speaker);

    /**
     * Returns whether this speaker should be placed on the left or right hand side of the
     * window. By default our messages are left side, everyone else is right side.
     */
    protected boolean isLeftSide (Handle speaker)
    {
        return speaker.equals(_ctx.getUserObject().handle);
    }

    /**
     * Renders the chat bubble backgrounds.
     */
    protected void createBubbleBackgrounds ()
    {
        BufferedImage img = new BufferedImage(90, 45, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gfx = img.createGraphics();
        Area bubble = new Area(new RoundRectangle2D.Float(8, 0, 81, 44, 30, 30));
        bubble.add(new Area(new Arc2D.Float(-12, -8, 24, 24, -40f, 30f, Arc2D.PIE)));
        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gfx.setColor(new Color(0xF1EFE3));
        gfx.fill(bubble);
        gfx.setColor(new Color(0x896A4B));
        gfx.draw(bubble);

        // flip image up and down for first and rest of sent
        _sfbg = new ImageBackground(ImageBackground.FRAME_XY, new BImage(img, false));
        _srbg = new ImageBackground(ImageBackground.FRAME_XY, new BImage(img, true));

        // flip left-to-right for received
        mirrorImage(img);
        _rfbg = new ImageBackground(ImageBackground.FRAME_XY, new BImage(img, false));
        _rrbg = new ImageBackground(ImageBackground.FRAME_XY, new BImage(img, true));
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

    /** Displays a player's avatar icon and pops up a player menu when clicked. */
    protected static class PlayerLabel extends BLabel
    {
        public PlayerLabel (BangContext ctx, Handle handle, BIcon icon) {
            super(icon);
            _ctx = ctx;
            _handle = handle;
        }

        public boolean dispatchEvent (BEvent event) {
            // pop up a player menu if they click the mouse
            return PlayerPopupMenu.checkPopup(_ctx, getWindow(), event, _handle, true) ||
                super.dispatchEvent(event);
        }

        protected BangContext _ctx;
        protected Handle _handle;
    }

    /** Used to keep track of speaker's icons. */
    protected static class Speaker
        implements ResultListener<BImage>
    {
        public Handle handle;

        public Speaker (Handle handle) {
            this.handle = handle;
            // avatar = new AvatarView(ctx, 8, false);
        }

        public void setAvatar (AvatarInfo avatar)
        {
            if (_avatar == null || !_avatar.equals(avatar)) {
                _avatar = avatar;
                _avicon = null;
            }
        }

        public AvatarInfo getAvatar ()
        {
            return _avatar;
        }

        public void loadAvatar (BangContext ctx, boolean mirror)
        {
            if (_avicon == null && _avatar != null) {
                AvatarView.getImage(
                    ctx, _avatar, AvatarLogic.WIDTH/8, AvatarLogic.HEIGHT/8, mirror, this);
            }
        }

        public BLabel createLabel (BangContext ctx, boolean showName)
        {
            BLabel label;
            int awid = AvatarLogic.WIDTH/8, ahei = AvatarLogic.HEIGHT/8;
            if (_avicon == null) {
                label = new PlayerLabel(ctx, handle, new BlankIcon(awid, ahei));
                if (_penders == null) {
                    _penders = new ArrayList<BLabel>();
                }
                _penders.add(label);
            } else {
                label = new PlayerLabel(ctx, handle, _avicon);
            }
            label.setStyleClass("chat_speaker_label");
            label.setOrientation(BLabel.VERTICAL);
            label.setIconTextGap(0);
            if (showName) {
                label.setText(handle.toString());
            }
            return label;
        }

        public boolean equals (Object other) {
            return handle.equals(((Speaker)other).handle);
        }

        public void requestCompleted (BImage image)
        {
            _avicon = new ImageIcon(image);
            if (_penders != null) {
                for (BLabel label : _penders) {
                    label.setIcon(_avicon);
                }
                _penders = null;
            }
        }

        public void requestFailed (Exception cause)
        {
            // not called
        }

        protected AvatarInfo _avatar;
        protected BIcon _avicon;
        protected ArrayList<BLabel> _penders;
    }

    /** A chat entry that displays an avatar icon along with one or more messages in bubbles. */
    protected class ChatEntry extends BContainer
    {
        /** The speaker of this entry. */
        public Speaker speaker;

        public ChatEntry (Speaker speaker, boolean left)
        {
            this.speaker = speaker;
            _left = left;

            GroupLayout layout = GroupLayout.makeHoriz(
                GroupLayout.STRETCH, GroupLayout.CENTER, GroupLayout.NONE);
            final int hgap = layout.getGap();
            layout.setOffAxisJustification(GroupLayout.TOP);
            setLayoutManager(layout);

            layout = GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP, GroupLayout.CONSTRAIN);
            layout.setOffAxisJustification(_left ? GroupLayout.LEFT : GroupLayout.RIGHT);
            add(_mcont = new BContainer(layout) {
                protected Dimension computePreferredSize (
                    int whint, int hhint) {
                    Dimension ldim = _slabel.getPreferredSize(-1, -1);
                    return super.computePreferredSize(
                        _vport.getWidth() - _vport.getInsets().getHorizontal() -
                        ChatEntry.this.getInsets().getHorizontal() - getInsets().getHorizontal() -
                        hgap - ldim.width, hhint);
                }
            });

            add(_left ? 0 : 1, _slabel = speaker.createLabel(_ctx, _showNames), GroupLayout.FIXED);
        }

        public void addMessage (String msg)
        {
            BLabel label = new BLabel(msg, _left ? "sent_chat_bubble" : "received_chat_bubble") {
                protected void wasAdded() {
                    super.wasAdded();
                    setBackground(DEFAULT, _mcont.getComponent(0) == this ?
                                  (_left ? _sfbg : _rfbg) : (_left ? _srbg : _rrbg));
                }
            };
            _mcont.add(label);
        }

        protected BContainer _mcont;
        protected BLabel _slabel;
        protected boolean _left;
    }

    protected BangContext _ctx;
    protected boolean _showNames;
    protected ChatEntryBuilder _last;

    /** Maps speakers to avatars and icons. */
    protected HashMap<Handle,Speaker> _speakers = new HashMap<Handle,Speaker>();

    /** Chat bubble backgrounds for sent and received messages, first bubble in sequence and rest
     * of bubbles in sequence. */
    protected ImageBackground _sfbg, _srbg, _rfbg, _rrbg;

    /** The max number of chat entries to allow. */
    protected static final int MAX_VALUES = 250;

    /** The number of chat entries we remove when we've gone over the max. */
    protected static final int REMOVE_SIZE = 5;
}
