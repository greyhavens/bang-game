//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BMenuItem;
import com.jmex.bui.BPopupMenu;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.presents.client.InvocationService;
import com.threerings.util.MessageBundle;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.bang.chat.client.PardnerChatView;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.InvitePardnerDialog;
import com.threerings.bang.client.PlayerService;
import com.threerings.bang.client.WantedPosterView;
import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangOccupantInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.SaloonCodes;

import static com.threerings.bang.Log.log;

/**
 * Represents one player in the friendly folks list.
 */
public class FolkCell extends BContainer
    implements Comparable<FolkCell>
{
    public FolkCell (BangContext ctx, Handle handle, boolean isPardner, boolean isFriend)
    {
        super(GroupLayout.makeHoriz(GroupLayout.LEFT));
        setPreferredSize(new Dimension(200, 18));

        _ctx = ctx;
        _chat = _ctx.getBangClient().getPardnerChatView();
        _handle = handle;
        _isPardner = isPardner;
        _isFriend = isFriend;
        _isSelf = _ctx.getUserObject().handle.equals(handle);

        add(new BLabel(_handle.toString(), "folk_label"));
        if (_isPardner) {
            BLabel pardIcon = new BLabel("", "folk_pardner");
            pardIcon.setIcon(new ImageIcon(_ctx.loadImage("ui/saloon/pardners_only.png")));
            add(pardIcon);
        }
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
     * A popup menu that is displayed when a player right clicks on an entry
     * in the friendly folks list in the Saloon.
     */
    protected class FolkPopupMenu extends BPopupMenu
        implements ActionListener
    {
        /**
         * Creates a popup menu for the specified player.
         */
        public FolkPopupMenu (BWindow parent, Handle handle)
        {
            super(parent);

            setStyleClass("folk_menu");
            addListener(this);
            setLayer(BangUI.POPUP_MENU_LAYER);

            MessageBundle bangMsgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
            MessageBundle saloonMsgs = _ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);

            // add their name as a non-menu item
            String title = "@=u(" + handle.toString() + ")";
            add(new BLabel(title, "popupmenu_title"));

            if (!_isSelf) {
                addMenuItem(new BMenuItem(saloonMsgs.get("m.folk_chat"), "chat"));
            }

            // add an item for viewing their wanted poster
            addMenuItem(new BMenuItem(bangMsgs.get("m.pm_view_poster"), "view_poster"));

            // add an item for muting/unmuting
            if (!_isSelf) {
                String mute = _ctx.getMuteDirector().isMuted(handle) ? "unmute" : "mute";
                addMenuItem(new BMenuItem(bangMsgs.get("m.pm_" + mute), mute));
            }

            if (_isFriend) {
                // add an item for removing them from the list
                addMenuItem(new BMenuItem(saloonMsgs.get("m.folk_remove"), "remove"));
            }
            if (!_isPardner && !_isSelf) {
                // add an item for inviting them to be our pardner
                addMenuItem(new BMenuItem(bangMsgs.get("m.pm_invite_pardner"), "invite_pardner"));
            }
        }

        // from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            if ("chat".equals(event.getAction())) {
                _chat.openUserTab(_handle, getAvatar(), true);

            } else if ("remove".equals(event.getAction())) {
                OccupantInfo info =
                    _ctx.getOccupantDirector().getOccupantInfo(_handle);
                if (info == null) {
                    return; // shouldn't happen
                }

                int playerId = ((BangOccupantInfo) info).playerId;
                InvocationService.ConfirmListener listener =
                    new InvocationService.ConfirmListener() {
                    public void requestProcessed () {
                        // TODO: confirmation?
                    }
                    public void requestFailed(String cause) {
                        log.warning("Folk request failed: " + cause);
                        _ctx.getChatDirector().displayFeedback(
                            GameCodes.GAME_MSGS, MessageBundle.tcompose(
                                "m.folk_note_failed", cause));
                    }
                };

                PlayerService psvc = (PlayerService)
                    _ctx.getClient().requireService(PlayerService.class);
                psvc.noteFolk(_ctx.getClient(), playerId,
                              PlayerService.FOLK_NEUTRAL, listener);

            } else if ("mute".equals(event.getAction())) {
                _ctx.getMuteDirector().setMuted(_handle, true);

            } else if ("unmute".equals(event.getAction())) {
                _ctx.getMuteDirector().setMuted(_handle, false);

            } else if ("invite_pardner".equals(event.getAction())) {
                _ctx.getBangClient().displayPopup(
                    new InvitePardnerDialog(_ctx, null, _handle), true, 400);

            } else if ("view_poster".equals(event.getAction())) {
                WantedPosterView.displayWantedPoster(_ctx, _handle);
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
