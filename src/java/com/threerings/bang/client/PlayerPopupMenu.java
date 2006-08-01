//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.BMenuItem;
import com.jmex.bui.BPopupMenu;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

/**
 * A popup menu that can (and should) be displayed any time a player right
 * clicks on another player's avatar.
 */
public class PlayerPopupMenu extends BPopupMenu
    implements ActionListener
{
    /**
     * Creates a popup menu for the specified player.
     */
    public PlayerPopupMenu (BangContext ctx, BWindow parent, Handle handle)
    {
        super(parent);

        setStyleClass("player_menu");
        _ctx = ctx;
        _handle = handle;
        addListener(this);

        MessageBundle msgs = ctx.getMessageManager().getBundle(
            BangCodes.BANG_MSGS);
        PlayerObject self = _ctx.getUserObject();

        // add their name as a non-menu item
        String title = "@=u(" + handle.toString() + ")";
        add(new BLabel(title, "player_menu_title"));

        // add an item for viewing their wanted poster
        BMenuItem item;
        addMenuItem(
            item = new BMenuItem(msgs.get("m.pm_view_poster"), "view_poster"));
        item.setEnabled(false);

        // stop here if this is us
        if (self.handle.equals(handle)) {
            return;
        }

        // add an item for muting/unmuting
        String mute = _ctx.getMuteDirector().isMuted(handle) ?
            "unmute" : "mute";
        addMenuItem(new BMenuItem(msgs.get("m.pm_" + mute), mute));

        // add an item for inviting them to be our pardner
        if (!_ctx.getUserObject().pardners.containsKey(handle)) {
            addMenuItem(new BMenuItem(msgs.get("m.pm_invite_pardner"),
                                      "invite_pardner"));
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
                new InvitePardnerDialog(_ctx, _handle), true, 400);

        } else if ("view_poster".equals(event.getAction())) {
            // TODO
        }
    }

    protected BangContext _ctx;
    protected Handle _handle;
}
