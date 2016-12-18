//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BMenuItem;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.PlayerPopupMenu;
import com.threerings.bang.client.bui.RequestDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;
import com.threerings.bang.gang.util.GangUtil;

import static com.threerings.bang.Log.*;

/**
 * A pop-up menu to display when the user right-clicks on a fellow gang-member.
 */
public class MemberPopupMenu extends PlayerPopupMenu
    implements GangCodes, HideoutCodes
{
    /**
     * Checks for a mouse click and popups up the specified member's context menu if appropriate.
     */
    public static boolean checkPopup (
        BangContext ctx, BWindow parent, BEvent event, GangMemberEntry member, boolean allowMute,
        StatusLabel status)
    {
        if (event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            if (mev.getType() == MouseEvent.MOUSE_PRESSED) {
                MemberPopupMenu menu = new MemberPopupMenu(ctx, parent, member, allowMute, status);
                menu.popup(mev.getX(), mev.getY(), false);
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a popup menu for the specified member.
     */
    public MemberPopupMenu (
        BangContext ctx, BWindow parent, GangMemberEntry member, boolean allowMute,
        StatusLabel status)
    {
        super(ctx, parent, member.handle, allowMute);
        _member = member;
        _status = status;

        Object plobj = ctx.getLocationDirector().getPlaceObject();
        if (!(plobj instanceof HideoutObject)) {
            log.warning("Created member pop-up outside of hideout", "plobj", plobj);
            removeAll();
            return;
        }
        _hideoutobj = (HideoutObject)plobj;

        // set the status label now that we have access to the entry
        String statstr = _member.isActive() ?
            (_member.isOnline() ? "m.online" : "m.offline") : "m.inactive";
        _slabel.setText(_ctx.xlate(HIDEOUT_MSGS, statstr));

        // add a chat option for online members (except for pardners, who have one already)
        PlayerObject user = _ctx.getUserObject();
        if (member.isOnline() && !user.pardners.containsKey(member.handle) &&
                !user.handle.equals(member.handle)) {
            addMenuItem(new BMenuItem(_ctx.xlate(HIDEOUT_MSGS, "m.chat_member"), "chat_pardner"));
        }

        if (!_member.canChangeStatus(_ctx.getUserObject())) {
            return;
        }
        for (int ii = RANK_COUNT - 1; ii >= 0; ii--) {
            if (ii == _member.rank) {
                continue;
            }
            String msg = MessageBundle.compose(ii > _member.rank ? "m.promote" : "m.demote",
                MessageBundle.qualify(GANG_MSGS, XLATE_RANKS[ii]));
            addMenuItem(new BMenuItem(_ctx.xlate(HIDEOUT_MSGS, msg), "rank_" + ii));
        }
        addMenuItem(new BMenuItem(_ctx.xlate(HIDEOUT_MSGS, "m.change_title"), "change_title"));
        addMenuItem(new BMenuItem(_ctx.xlate(HIDEOUT_MSGS, "m.expel"), "expel"));
    }

    @Override // documentation inherited
    public void actionPerformed (ActionEvent event)
    {
        super.actionPerformed(event);
        String action = event.getAction();
        if (action.startsWith("rank_")) {
            changeMemberRank(Byte.parseByte(action.substring(5)));
        } else if (action.equals("expel")) {
            expelMember();
        } else if (action.equals("change_title")) {
            changeMemberTitle();
        }
    }

    @Override // documentation inherited
    protected BComponent createTitle ()
    {
        BContainer cont = GroupLayout.makeHBox(GroupLayout.LEFT);
        cont.add(super.createTitle());
        cont.add(_slabel = new BLabel("", "popupmenu_subtitle"));
        return cont;
    }

    @Override // documentation inherited
    protected boolean shouldShowGangInvite ()
    {
        return false;
    }

    protected void changeMemberRank (final byte nrank)
    {
        String thandle = MessageBundle.taint(_member.handle),
            rankmsg = MessageBundle.qualify(GANG_MSGS, XLATE_RANKS[nrank]),
            rankdesc = MessageBundle.qualify(GANG_MSGS, XLATE_RANKS[nrank] + "_desc");
        boolean promote = nrank > _member.rank;
        String confirm = MessageBundle.compose(
            "m.confirm_" + (promote ? "promote" : "demote"), thandle, rankmsg, rankdesc);
        String success = MessageBundle.compose("m.changed_rank", thandle, rankmsg);
        _ctx.getBangClient().displayPopup(
            new RequestDialog(_ctx, HIDEOUT_MSGS, confirm, "m.ok", "m.cancel", success, _status) {
                protected void fireRequest (Object result) {
                    _hideoutobj.service.changeMemberRank(_member.handle, nrank, this);
                }
            }, true, 400);
    }

    protected void expelMember ()
    {
        int[] refund = _member.getDonationReimbursement();
        String warning = (refund[0] == 0 && refund[1] == 0) ?
            MessageBundle.taint("") :
            MessageBundle.compose("m.expel_reimburse",
                MessageBundle.taint(_member.getDonationReimbursementPct()),
                GangUtil.getMoneyDesc(refund[0], refund[1], 0));

        String confirm = MessageBundle.compose("m.confirm_expel",
            MessageBundle.taint(_member.handle), warning);
        String success = MessageBundle.tcompose("m.expelled", _member.handle);
        _ctx.getBangClient().displayPopup(
            new RequestDialog(_ctx, HIDEOUT_MSGS, confirm, "m.ok", "m.cancel", success, _status) {
                protected void fireRequest (Object result) {
                    _hideoutobj.service.expelMember(_member.handle, this);
                }
            }, true, 400);
    }

    protected void changeMemberTitle ()
    {
        String title = MessageBundle.tcompose("t.change_title", _member.handle);
        _ctx.getBangClient().displayPopup(
                new TitleWindow(title, _member.title), true, 600);
    }

    protected class TitleWindow extends BDecoratedWindow
        implements ActionListener, HideoutService.ConfirmListener
    {
        public TitleWindow (String title, int tidx)
        {
            super(_ctx.getStyleSheet(), _ctx.xlate(HIDEOUT_MSGS, title));
            setModal(true);

            BContainer tcont = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER));
            tcont.add(new BLabel(_ctx.xlate(HIDEOUT_MSGS, "m.new_title")));
            _titles = new BComboBox();
            for (int ii = 0; ii <= GangCodes.TITLES_COUNT; ii++) {
                _titles.addItem(_ctx.xlate(GangCodes.GANG_MSGS, "m.title." + ii));
            }
            _titles.selectItem(tidx);
            tcont.add(_titles);
            add(tcont);

            BContainer bcont = new BContainer(GroupLayout.makeHoriz(GroupLayout.CENTER));
            bcont.add(new BButton(_ctx.xlate(HIDEOUT_MSGS, "m.update"), this, "update"));
            bcont.add(new BButton(_ctx.xlate(HIDEOUT_MSGS, "m.cancel"), this, "cancel"));
            add(bcont, GroupLayout.FIXED);
        }

        // documentation inherited from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            _ctx.getBangClient().clearPopup(this, true);
            if (event.getAction().equals("update")) {
                _hideoutobj.service.changeMemberTitle(
                    _member.handle, _titles.getSelectedIndex(), this);
            }
        }

        // documentation inherited from interface InvocationService.ConfirmListener
        public void requestProcessed ()
        {
            _status.setStatus(HIDEOUT_MSGS, MessageBundle.tcompose(
                        "m.title_change_success", _member.handle.toString(),
                        _titles.getSelectedItem()), false);
        }

        // documentation inherited from interface InvocationService.ConfirmListener
        public void requestFailed (String cause)
        {
            _status.setStatus(HIDEOUT_MSGS, cause, true);
        }

        protected BComboBox _titles;
    }

    protected GangMemberEntry _member;
    protected HideoutObject _hideoutobj;
    protected StatusLabel _status;

    protected BLabel _slabel;
}
