//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.event.ActionEvent;

import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.bang.client.PardnerView;
import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.bui.RequestDialog;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;

/**
 * Displays the members of the player's gang.
 */
public class MemberView extends PardnerView
    implements GangCodes, HideoutCodes
{
    public MemberView (
        BangContext ctx, StatusLabel status, HideoutObject hideoutobj, GangObject gangobj)
    {
        super(ctx, status);
        _hideoutobj = hideoutobj;
        _gangobj = gangobj;
    }
    
    @Override // documentation inherited
    public void actionPerformed (ActionEvent event)
    {
        super.actionPerformed(event);
        if (event.getSource() == _change) {
            // show buttons for all but the member's current rank
            final MemberIcon icon = (MemberIcon)getSelectedIcon();
            final byte[] ranks = new byte[RANK_COUNT - 1];
            String[] buttons = new String[RANK_COUNT];
            for (byte ii = (byte)(RANK_COUNT - 1), idx = 0; ii >= 0 ; ii--) {
                if (ii == icon.entry.rank) {
                    continue;
                }
                ranks[idx] = ii;
                buttons[idx++] = MessageBundle.compose(
                    (ii < icon.entry.rank) ? "m.demote_member" : "m.promote_member",
                    MessageBundle.qualify(GANG_MSGS, XLATE_RANKS[ii]));
            }
            buttons[RANK_COUNT - 1] = "m.cancel";
            OptionDialog.showConfirmDialog(_ctx, HIDEOUT_MSGS,
                MessageBundle.tcompose("m.confirm_change", icon.entry.handle), buttons,
                new OptionDialog.ResponseReceiver() {
                    public void resultPosted (int button, Object result) {
                        if (button < RANK_COUNT - 1) {
                            changeMemberRank(icon.entry.handle, ranks[button]);
                        }
                    }
                });
        }
    }
    
    @Override // documentation inherited
    protected String getAddLabelMessage ()
    {
        return MessageBundle.qualify(HIDEOUT_MSGS, "m.member_add");
    }
    
    @Override // documentation inherited
    protected String getConfirmRemoveMessage ()
    {
        return MessageBundle.qualify(HIDEOUT_MSGS, "m.confirm_remove"); 
    }
    
    @Override // documentation inherited
    protected void addPardnerButtons (BContainer bcont)
    {
        bcont.add(_change = new BButton(_ctx.xlate(HIDEOUT_MSGS,
            "m.member_change"), this, "change"));
        bcont.add(_remove = new BButton(_ctx.xlate(HIDEOUT_MSGS,
            "m.member_remove"), this, "remove"));
    }
    
    @Override // documentation inherited
    protected RequestDialog createInviteDialog (Handle handle)
    {
        return new InviteMemberDialog(_ctx, _status, handle);
    }
    
    @Override // documentation inherited
    protected DObject getPardnerObject ()
    {
        return _gangobj;
    }
    
    @Override // documentation inherited
    protected String getPardnerField ()
    {
        return GangObject.MEMBERS;
    }
    
    @Override // documentation inherited
    protected DSet<GangMemberEntry> getPardnerEntries ()
    {
        return _gangobj.members;
    }
    
    @Override // documentation inherited
    protected PardnerIcon createPardnerIcon (PardnerEntry entry)
    {
        return new MemberIcon((GangMemberEntry)entry);
    }
    
    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        updateMemberControls();
        _change.setEnabled(false);
        _ctx.getUserObject().addListener(_rlist);
    }
    
    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _ctx.getUserObject().removeListener(_rlist);
    }
    
    @Override // documentation inherited
    protected void updateSelectionControls (PardnerEntry pentry)
    {
        super.updateSelectionControls(pentry);
        GangMemberEntry entry = (GangMemberEntry)pentry;
        PlayerObject user = _ctx.getUserObject();
        _chat.setEnabled(entry.isAvailable() && entry.playerId != user.playerId);
        boolean canChange = entry.canChangeStatus(user);
        _change.setEnabled(canChange);
        _remove.setEnabled(canChange);
    }
    
    @Override // documentation inherited
    protected void iconDeselected (SelectableIcon icon)
    {
        super.iconDeselected(icon);
        _change.setEnabled(false);
        _remove.setEnabled(false);
    }
    
    /**
     * Updates the visibility of the member controls according to the
     * rank of the user.
     */
    protected void updateMemberControls ()
    {
        PlayerObject user = _ctx.getUserObject();
        boolean canChange = (user.gangRank == LEADER_RANK);
        _change.setVisible(canChange);
        _remove.setVisible(canChange);
        _acont.setVisible(user.canRecruit());
    }
    
    /**
     * Submits a request to change a member's rank.
     */
    protected void changeMemberRank (final Handle handle, byte rank)
    {
        _hideoutobj.service.changeMemberRank(_ctx.getClient(), handle, rank,
            new HideoutService.ConfirmListener() {
                public void requestProcessed () {
                    _status.setStatus(HIDEOUT_MSGS, MessageBundle.tcompose(
                        "m.member_changed", handle), false);
                }
                public void requestFailed (String cause) {
                    _status.setStatus(HIDEOUT_MSGS, cause, true);
                }
            });
    }
    
    @Override // documentation inherited
    protected void removePardner (final Handle handle)
    {
         _hideoutobj.service.expelMember(_ctx.getClient(), handle,
            new HideoutService.ConfirmListener() {
                public void requestProcessed () {
                    _status.setStatus(HIDEOUT_MSGS, MessageBundle.tcompose(
                        "m.member_removed", handle), false);
                }
                public void requestFailed (String cause) {
                    _status.setStatus(HIDEOUT_MSGS, cause, true);
                }
            });
    }
    
    /** Displays a single member. */
    protected class MemberIcon extends PardnerIcon
    {
        GangMemberEntry entry;
        
        public MemberIcon (GangMemberEntry entry)
        {
            super(entry);
            this.entry = entry;
        }
        
        @Override // documentation inherited
        public void update (PardnerEntry nentry)
        {
            super.update(nentry);
            entry = (GangMemberEntry)nentry;
        }
    }
    
    protected HideoutObject _hideoutobj;
    protected GangObject _gangobj;
    
    protected BButton _change;
 
    /** Updates the member controls when the user's rank changes. */   
    protected AttributeChangeListener _rlist = new AttributeChangeListener() {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(PlayerObject.GANG_RANK)) {
                updateMemberControls();
            }
        }
    };
}
