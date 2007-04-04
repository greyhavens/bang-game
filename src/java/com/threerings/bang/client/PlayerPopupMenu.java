//
// $Id$

package com.threerings.bang.client;

import java.net.URL;
import java.util.logging.Level;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BMenuItem;
import com.jmex.bui.BPopupMenu;
import com.jmex.bui.BTextField;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.layout.GroupLayout;

import com.samskivert.util.ResultListener;

import com.threerings.util.BrowserUtil;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.gang.client.InviteMemberDialog;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangOccupantInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PardnerEntry;
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
     * Assumes that since we're looking the player up by oid, we're in the same room as them.
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
     *
     * @param isPresent indicates whether or not the other player is present (chatting with us)
     * versus on a high score list or something, for whom it would not make sense to provide
     * options to mute or complain about the player.
     */
    public static boolean checkPopup (BangContext ctx, BWindow parent, BEvent event, Handle handle,
                                      boolean isPresent)
    {
        if (event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            if (mev.getType() == MouseEvent.MOUSE_PRESSED) {
                PlayerPopupMenu menu = new PlayerPopupMenu(ctx, parent, handle, isPresent);
                menu.popup(mev.getX(), mev.getY(), false);
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a popup menu for the specified player.
     */
    public PlayerPopupMenu (BangContext ctx, BWindow parent, Handle handle, boolean isPresent)
    {
        super(parent);

        _ctx = ctx;
        _handle = handle;
        addListener(this);
        setLayer(BangUI.POPUP_MENU_LAYER);

        // add their name as a non-menu item
        String title = "@=u(" + handle.toString() + ")";
        add(new BLabel(title, "popupmenu_title"));

        // add our menu items
        addMenuItems(isPresent);
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("mute".equals(event.getAction())) {
            _ctx.getMuteDirector().setMuted(_handle, true);

        } else if ("unmute".equals(event.getAction())) {
            _ctx.getMuteDirector().setMuted(_handle, false);

        } else if ("complain".equals(event.getAction())) {
            showComplainDialog();

        } else if ("chat_pardner".equals(event.getAction())) {
            PardnerEntry entry = _ctx.getUserObject().pardners.get(_handle);
            if (entry != null) {
                _ctx.getBangClient().getPardnerChatView().display(entry.handle, entry.avatar, true);
            }

        } else if ("watch_pardner".equals(event.getAction())) {
            PardnerEntry entry = _ctx.getUserObject().pardners.get(_handle);
            if (entry != null && entry.gameOid > 0) {
                _ctx.getLocationDirector().moveTo(entry.gameOid);
            }

        } else if ("remove_pardner".equals(event.getAction())) {
            String msg = MessageBundle.tcompose("m.confirm_remove", _handle);
            OptionDialog.showConfirmDialog(
                _ctx, BangCodes.BANG_MSGS, msg, new OptionDialog.ResponseReceiver() {
                public void resultPosted (int button, Object result) {
                    if (button == OptionDialog.OK_BUTTON) {
                        removePardner();
                    }
                }
            });

        } else if ("invite_pardner".equals(event.getAction())) {
            _ctx.getBangClient().displayPopup(
                new InvitePardnerDialog(_ctx, null, _handle), true, 400);

        } else if ("invite_member".equals(event.getAction())) {
            _ctx.getBangClient().displayPopup(
                new InviteMemberDialog(_ctx, null, _handle), true, 400);

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
                URL url = new URL(DeploymentConfig.getNewAccountURL(null),
                                  "/office/player.xhtml?handle=" + _handle.toString());
                BrowserUtil.browseURL(url, listener);
            } catch (Exception e) {
                listener.requestFailed(e);
            }
        }
    }

    protected void addMenuItems (boolean isPresent)
    {
        MessageBundle msgs = _ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        PlayerObject self = _ctx.getUserObject();

        // add an item for viewing their wanted poster
        addMenuItem(new BMenuItem(msgs.get("m.pm_view_poster"), "view_poster"));

        // if we're an admin/support, add a link to their admin account page
        if (self.tokens.isSupport()) {
            addMenuItem(new BMenuItem(msgs.get("m.pm_view_account"), "view_account"));
        }

        // stop here if this is us or we're anonymous
        if (self.tokens.isAnonymous() || self.handle.equals(_handle)){
            return;
        }

        // add an item for viewing their wanted poster
        if (isPresent) {
            addMenuItem(new BMenuItem(msgs.get("m.pm_register_complaint"), "complain"));
        }

        // if they're our pardner, add some pardner-specific items
        PardnerEntry entry = self.pardners.get(_handle);
        if (entry != null) {
            if (entry.isAvailable()) {
                addMenuItem(new BMenuItem(msgs.get("m.pm_chat_pardner"), "chat_pardner"));
            }
            if (entry.gameOid > 0) {
                addMenuItem(new BMenuItem(msgs.get("m.pm_watch_pardner"), "watch_pardner"));
            }
            addMenuItem(new BMenuItem(msgs.get("m.pm_remove_pardner"), "remove_pardner"));

        } else if (isPresent) {
            // otherwise add an item for inviting them to be our pardner
            addMenuItem(new BMenuItem(msgs.get("m.pm_invite_pardner"), "invite_pardner"));
        }

        // add gang invitation option if they're either present or a pardner
        if (shouldShowGangInvite() && (isPresent || entry != null)) {
            addMenuItem(new BMenuItem(_ctx.xlate(BangCodes.BANG_MSGS, "m.pm_invite_member"),
                "invite_member"));
        }

        // add an item for muting/unmuting (always allow unmuting, only allow muting if the caller
        // indicates that we're in a context where it is appropriate)
        boolean muted = _ctx.getMuteDirector().isMuted(_handle);
        if (muted || isPresent) {
            String mute = muted ? "unmute" : "mute";
            addMenuItem(new BMenuItem(msgs.get("m.pm_" + mute), mute));
        }
    }

    /**
     * Checks whether we should show the "invite into gang" option.
     */
    protected boolean shouldShowGangInvite ()
    {
        return _ctx.getUserObject().canRecruit();
    }

    protected void removePardner ()
    {
        PlayerService psvc = ((PlayerService)_ctx.getClient().requireService(PlayerService.class));
        psvc.removePardner(_ctx.getClient(), _handle, new PlayerService.ConfirmListener() {
            public void requestProcessed () {
                String msg = MessageBundle.tcompose("m.pardner_removed", _handle);
                _ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, msg);
            }
            public void requestFailed (String cause) {
                _ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, cause);
            }
        });
    }

    protected void showComplainDialog ()
    {
        String title = MessageBundle.tcompose("m.comp_title", _handle);
        title = _ctx.xlate(BangCodes.BANG_MSGS, title);
        final BDecoratedWindow cdiag = BangUI.createDialog(title);

        cdiag.add(new BLabel(_ctx.xlate(BangCodes.BANG_MSGS, "m.comp_intro"), "dialog_text_left"));
        final BTextField reason = new BTextField("", MAX_SUBJECT_LENGTH);
        cdiag.add(reason, GroupLayout.FIXED);
        reason.requestFocus();
        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        cdiag.add(buttons, GroupLayout.FIXED);

        ActionListener listener = new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                if (event.getAction().equals("submit")) {
                    submitComplaint(reason.getText());
                }
                _ctx.getBangClient().clearPopup(cdiag, true);
            }
        };
        BButton submit = new BButton(
            _ctx.xlate(BangCodes.BANG_MSGS, "m.comp_submit"), listener, "submit");
        buttons.add(submit);
        buttons.add(new BButton(_ctx.xlate(BangCodes.BANG_MSGS, "m.cancel"), listener, "cancel"));
        // disable the submit button until a reason is entered

        new EnablingValidator(reason, submit);
        _ctx.getBangClient().displayPopup(cdiag, true, 600);
    }

    protected void submitComplaint (String reason)
    {
        PlayerService psvc = ((PlayerService)_ctx.getClient().requireService(PlayerService.class));
        PlayerService.ConfirmListener listener =new PlayerService.ConfirmListener() {
            public void requestProcessed () {
                String msg = MessageBundle.tcompose("m.comp_submitted", _handle);
                _ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, msg);
            }
            public void requestFailed (String cause) {
                _ctx.getChatDirector().displayFeedback(BangCodes.BANG_MSGS, cause);
            }
        };
        psvc.registerComplaint(_ctx.getClient(), _handle, reason, listener);
    }

    protected BangContext _ctx;
    protected Handle _handle;

    protected static final int MAX_SUBJECT_LENGTH = 200;
}
