//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BMenuItem;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.MouseEvent;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.PlayerPopupMenu;
import com.threerings.bang.client.bui.RequestDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;

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
            log.warning("Created member pop-up outside of hideout [plobj=" +
                plobj + "].");
            removeAll();
            return;
        }
        _hideoutobj = (HideoutObject)plobj;
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
        }
    }
    
    @Override // documentation inherited
    protected void addGangMenuItems ()
    {
        if (!_member.canChangeStatus(_ctx.getUserObject())) {
            return;
        }
        for (int ii = RANK_COUNT - 1; ii >= 0; ii--) {
            if (ii == _member.rank) {
                continue;
            }
            String msg = MessageBundle.compose(ii > _member.rank ? "m.promote" : "m.demote",
                MessageBundle.qualify(GANG_MSGS, XLATE_RANKS[ii]));
            addMenuItem(new BMenuItem(msg, "rank_" + ii));
        }
        addMenuItem(new BMenuItem(_ctx.xlate(HIDEOUT_MSGS, "m.expel"), "expel"));
    }
    
    protected void changeMemberRank (final byte nrank)
    {
        String thandle = MessageBundle.taint(_member.handle),
            rankmsg = MessageBundle.qualify(GANG_MSGS, XLATE_RANKS[nrank]);
        String confirm = MessageBundle.compose(
            "m.confirm_" + (nrank > _member.rank ? "promote" : "demote"),
            thandle, rankmsg);
        String success = MessageBundle.compose("m.changed_rank", thandle, rankmsg);
        _ctx.getBangClient().displayPopup(
            new RequestDialog(_ctx, HIDEOUT_MSGS, confirm, "m.ok", "m.cancel", success, _status) {
                protected void fireRequest (Object result) {
                    _hideoutobj.service.changeMemberRank(
                        _ctx.getClient(), _member.handle, nrank, this);
                }        
            }, true, 400);
    }
    
    protected void expelMember ()
    {
        String confirm = MessageBundle.tcompose("m.confirm_expel", _member.handle),
            success = MessageBundle.tcompose("m.expelled", _member.handle);
        _ctx.getBangClient().displayPopup(
            new RequestDialog(_ctx, HIDEOUT_MSGS, confirm, "m.ok", "m.cancel", success, _status) {
                protected void fireRequest (Object result) {
                    _hideoutobj.service.expelMember(_ctx.getClient(), _member.handle, this);
                }        
            }, true, 400);
    }
    
    protected GangMemberEntry _member;
    protected HideoutObject _hideoutobj;
    protected StatusLabel _status;
}
