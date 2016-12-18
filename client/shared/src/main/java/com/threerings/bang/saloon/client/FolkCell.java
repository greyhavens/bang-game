//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BMenuItem;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.presents.client.InvocationService;
import com.threerings.util.MessageBundle;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.bang.chat.client.PardnerChatView;
import com.threerings.bang.client.PlayerPopupMenu;
import com.threerings.bang.client.PlayerService;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BangOccupantInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.SaloonCodes;

/**
 * Represents one player in the friendly folks list.
 */
public class FolkCell extends BContainer
    implements Comparable<FolkCell>
{
    public FolkCell (
        BangContext ctx, Handle handle, boolean isPardner, boolean isFriend, boolean isGangMember)
    {
        super(GroupLayout.makeHoriz(GroupLayout.STRETCH, GroupLayout.LEFT, GroupLayout.NONE));
        setPreferredSize(new Dimension(200, 18));
        setStyleClass("def_button");

        _ctx = ctx;
        _chat = _ctx.getBangClient().getPardnerChatView();
        _handle = handle;
        _isPardner = isPardner;
        _isFriend = isFriend;
        _isSelf = _ctx.getUserObject().handle.equals(handle);

        if (_isPardner) {
            BLabel pardIcon = new BLabel("", "folk_pardner");
            pardIcon.setIcon(new ImageIcon(_ctx.loadImage("ui/saloon/pardners_only.png")));
            add(pardIcon, GroupLayout.FIXED);
        }
        if (isGangMember) {
            add(new BLabel(new ImageIcon(_ctx.loadImage("ui/saloon/recruiting.png"))),
                    GroupLayout.FIXED);
        }
        BLabel folkLabel = new BLabel(_handle.toString(), "folk_label");
        folkLabel.setFit(BLabel.Fit.SCALE);
        add(folkLabel);
    }

    /**
     * Looks up and returns the proper avatar information for a player.
     */
    public AvatarInfo getAvatar ()
    {
        if (_isPardner) {
            PardnerEntry entry = _ctx.getUserObject().pardners.get(_handle);
            return entry != null ? entry.avatar : null;
        } else {
            OccupantInfo info = _ctx.getOccupantDirector().getOccupantInfo(_handle);
            return info != null ? ((BangOccupantInfo) info).avatar : null;
        }
    }

    // from interface Comparable
    public int compareTo (FolkCell cell)
    {
        if (_isPardner && !cell._isPardner) {
            return -1;
        }
        if (cell._isPardner && !_isPardner) {
            return 1;
        }
        return _handle.compareTo(cell._handle);
    }

    @Override // from BComponent
    public boolean dispatchEvent (BEvent event)
    {
        if (event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            if (mev.getType() == MouseEvent.MOUSE_PRESSED) {
                FolkPopupMenu menu = new FolkPopupMenu(getWindow(), _handle);
                menu.popup(mev.getX(), mev.getY(), false);
                return true;
            }
        }
        return super.dispatchEvent(event);
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other == null || !other.getClass().equals(getClass())) {
            return false;
        }
        return _isPardner == ((FolkCell) other)._isPardner &&
            _handle.equals(((FolkCell) other)._handle);
    }

    @Override //from Object
    public int hashCode ()
    {
        return (_handle.hashCode() << 1) + (_isPardner ? 1 : 0);
    }

    /**
     * A popup menu that is displayed when a player right clicks on an entry in the friendly folks
     * list in the Saloon.
     */
    protected class FolkPopupMenu extends PlayerPopupMenu
    {
        public FolkPopupMenu (BWindow parent, Handle handle)
        {
            super(FolkCell.this._ctx, parent, handle, true);
        }

        // from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            if ("chat".equals(event.getAction())) {
                _chat.openUserTab(_handle, true);

            } else if ("remove".equals(event.getAction())) {
                OccupantInfo info = _ctx.getOccupantDirector().getOccupantInfo(_handle);
                if (info == null) {
                    return; // shouldn't happen
                }

                int playerId = ((BangOccupantInfo) info).playerId;
                InvocationService.ConfirmListener listener =
                    new InvocationService.ConfirmListener() {
                    public void requestProcessed () {
                        // TODO: confirmation?
                    }
                    public void requestFailed (String cause) {
                        _ctx.getChatDirector().displayFeedback(
                            GameCodes.GAME_MSGS,
                            MessageBundle.tcompose("m.folk_note_failed", cause));
                    }
                };

                PlayerService psvc = _ctx.getClient().requireService(PlayerService.class);
                psvc.noteFolk(playerId, PlayerService.FOLK_NEUTRAL, listener);

            } else {
                super.actionPerformed(event);
            }
        }

        @Override // from PlayerPopupMenu
        protected void addMenuItems (boolean isPresent)
        {
            MessageBundle msgs = _ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);

            if (!_isSelf) {
                addMenuItem(new BMenuItem(msgs.get("m.folk_chat"), "chat"));
            }

            super.addMenuItems(isPresent);

            if (_isFriend) {
                // add an item for removing them from the list
                addMenuItem(new BMenuItem(msgs.get("m.folk_remove"), "remove"));
            }
        }
    }

    /** Provides access to client services. */
    protected BangContext _ctx;

    /** The chat view we use to chat with our folk. */
    protected PardnerChatView _chat;

    /** The handle of the player this cell represents */
    protected Handle _handle;

    /** Whether or not this cell's player is our pardner */
    protected boolean _isPardner;

    /** Whether or not this cell's player is our friend */
    protected boolean _isFriend;

    /** Whether or not this cell's player is ourself */
    protected boolean _isSelf;
}
