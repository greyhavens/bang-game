//
// $Id$

package com.threerings.bang.client;

import java.net.URL;
import java.util.logging.Level;

import com.jmex.bui.BLabel;
import com.jmex.bui.BMenuItem;
import com.jmex.bui.BPopupMenu;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;

import com.samskivert.util.ResultListener;

import com.threerings.util.BrowserUtil;
import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangOccupantInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;

import static com.threerings.bang.Log.log;

/**
 * A popup menu that can (and should) be displayed any time a player right clicks on another
 * player's avatar.
 */
public class PlayerPopupMenu extends BPopupMenu
    implements ActionListener
{
    /**
     * Checks for a mouse click and popups up the specified player's context menu if appropriate.
     * Assumes that since we're looking the player up by oid, we're in the same room as them and
     * want to allow them to be muted.
     */
    public static boolean checkPopup (
        BangContext ctx, BWindow parent, BEvent event, int playerOid)
    {
        // avoid needless occupant info lookups
        if (!(event instanceof MouseEvent)) {
            return false;
        }
        BangOccupantInfo boi = (BangOccupantInfo)
            ctx.getOccupantDirector().getOccupantInfo(playerOid);
        return (boi == null) ? false : checkPopup(ctx, parent, event, (Handle)boi.username, true);
    }

    /**
     * Checks for a mouse click and popups up the specified player's context menu if appropriate.
     */
    public static boolean checkPopup (BangContext ctx, BWindow parent, BEvent event, Handle handle,
                                      boolean allowMute)
    {
        if (event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            if (mev.getType() == MouseEvent.MOUSE_PRESSED) {
                PlayerPopupMenu menu = new PlayerPopupMenu(ctx, parent, handle, allowMute);
                menu.popup(mev.getX(), mev.getY(), false);
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a popup menu for the specified player.
     */
    public PlayerPopupMenu (BangContext ctx, BWindow parent, Handle handle, boolean allowMute)
    {
        super(parent);

        setStyleClass("player_menu");
        _ctx = ctx;
        _handle = handle;
        addListener(this);
        setLayer(BangUI.POPUP_MENU_LAYER);

        MessageBundle msgs = ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        PlayerObject self = _ctx.getUserObject();

        // add their name as a non-menu item
        String title = "@=u(" + handle.toString() + ")";
        add(new BLabel(title, "player_menu_title"));

        // add an item for viewing their wanted poster
        BMenuItem item;
        addMenuItem(item = new BMenuItem(msgs.get("m.pm_view_poster"), "view_poster"));

        // if we're an admin, add a link to their admin account page
        if (_ctx.getUserObject().tokens.isAdmin()) {
            addMenuItem(new BMenuItem(msgs.get("m.pm_view_account"), "view_account"));
        }

        // stop here if this is us
        if (self.handle.equals(handle)) {
            return;
        }

        // add an item for muting/unmuting (always allow unmuting, only allow muting if the caller
        // indicates that we're in a context where it is appropriate)
        boolean muted = _ctx.getMuteDirector().isMuted(handle);
        if (muted || allowMute) {
            String mute = muted ? "unmute" : "mute";
            addMenuItem(new BMenuItem(msgs.get("m.pm_" + mute), mute));
        }

        // add an item for inviting them to be our pardner
        if (!_ctx.getUserObject().pardners.containsKey(handle)) {
            addMenuItem(new BMenuItem(msgs.get("m.pm_invite_pardner"), "invite_pardner"));
        }
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("mute".equals(event.getAction())) {
            _ctx.getMuteDirector().setMuted(_handle, true);

        } else if ("unmute".equals(event.getAction())) {
            _ctx.getMuteDirector().setMuted(_handle, false);

        } else if ("invite_pardner".equals(event.getAction())) {
            _ctx.getBangClient().displayPopup(
                new InvitePardnerDialog(_ctx, null, _handle), true, 400);

        } else if ("view_poster".equals(event.getAction())) {
            WantedPosterView.displayWantedPoster(_ctx, _handle);

        } else if ("view_account".equals(event.getAction())) {
            ResultListener<Object> listener = new ResultListener<Object>() {
                public void requestCompleted (Object object) {
                    // nothing doing
                }
                public void requestFailed (Exception cause) {
                    log.log(Level.WARNING, "Failed to show account info.", cause);
                }
            };
            try {
                // the handle seems to get magically URL encoded; so we don't have to
                URL url = new URL(DeploymentConfig.getNewAccountURL(),
                                  "/office/player.xhtml?handle=" + _handle.toString());
                BrowserUtil.browseURL(url, listener);
            } catch (Exception e) {
                listener.requestFailed(e);
            }
        }
    }

    protected BangContext _ctx;
    protected Handle _handle;
}
