//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.Spacer;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.AvatarInfo;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BangUtil;

import com.threerings.bang.chat.client.ComicChatView;
import com.threerings.bang.chat.client.PlaceChatView;

import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;

/**
 * Displays the gang chat, along with a list of gang members in the hideout.
 */
public class GangChatView extends BContainer
    implements HideoutCodes
{
    public GangChatView (BangContext ctx, GangObject gangobj, StatusLabel status)
    {
        super(GroupLayout.makeVert(GroupLayout.TOP));
        _ctx = ctx;
        _gangobj = gangobj;
        _status = status;

        BScrollPane spane = new BScrollPane(_mcont = new BContainer(
            GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP, GroupLayout.NONE)));
        spane.setStyleClass("hideout_members");
        add(spane);

        add(_pcview = new PlaceChatView(_ctx, _ctx.xlate(HIDEOUT_MSGS, "m.gang_chat"),
            new ComicChatView(_ctx, PlaceChatView.TAB_SIZE, true) {
                protected AvatarInfo getSpeakerAvatar (Handle speaker) {
                    GangMemberEntry entry = _gangobj.members.get(speaker);
                    return (entry == null) ? null : entry.avatar;
                }
            }));
        _pcview.setPreferredSize(new Dimension(420, 358));
        _pcview.setSpeakService(_gangobj.speakService);
    }

    /**
     * Called to release out chat resources.
     */
    public void shutdown ()
    {
        _pcview.clearSpeakService();
        _pcview.shutdown();
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _gangobj.addListener(_memberlist);
        updateMembersInHideout();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _gangobj.removeListener(_memberlist);
    }

    /**
     * Updates the list of members in the hideout.
     */
    protected void updateMembersInHideout ()
    {
        _mcont.removeAll();

        // start with members in the current town, then do the others
        int ownIdx = BangUtil.getTownIndex(_ctx.getUserObject().townId);
        addTownMembers(ownIdx);
        for (int ii = 0; ii < BangCodes.TOWN_IDS.length; ii++) {
            if (ii != ownIdx) {
                _mcont.add(new Spacer(1, 5));
                addTownMembers(ii);
            }
        }
    }

    /**
     * Adds the display of members in the specified town's Hideout.
     */
    protected void addTownMembers (int townIdx)
    {
        BContainer cont = new BContainer(new TableLayout(2));
        for (GangMemberEntry member : _gangobj.members) {
            if (member.townIdx == townIdx && member.isInHideout()) {
                cont.add(new MemberLabel(_ctx, member, true, _status, "hideout_members_entry"));
            }
        }
        if (cont.getComponentCount() > 0) {
            String msg = MessageBundle.compose(
                "m.hideout_members", "m." + BangCodes.TOWN_IDS[townIdx]);
            _mcont.add(new BLabel(_ctx.xlate(HIDEOUT_MSGS, msg), "hideout_members_title"));
            _mcont.add(cont);
        }
    }

    protected BangContext _ctx;
    protected GangObject _gangobj;
    protected StatusLabel _status;

    protected BContainer _mcont;
    protected PlaceChatView _pcview;

    /** Listens to the gang object for changes in membership and avatar updates. */
    protected SetAdapter<GangMemberEntry> _memberlist = new SetAdapter<GangMemberEntry>() {
        public void entryAdded (EntryAddedEvent<GangMemberEntry> event) {
            if (!event.getName().equals(GangObject.MEMBERS)) {
                return;
            }
            GangMemberEntry entry = event.getEntry();
            if (entry.isInHideout()) {
                updateMembersInHideout();
            }
        }
        public void entryRemoved (EntryRemovedEvent<GangMemberEntry> event) {
            if (!event.getName().equals(GangObject.MEMBERS)) {
                return;
            }
            GangMemberEntry entry = event.getOldEntry();
            if (entry.isInHideout()) {
                updateMembersInHideout();
            }
        }
        public void entryUpdated (EntryUpdatedEvent<GangMemberEntry> event) {
            if (!event.getName().equals(GangObject.MEMBERS)) {
                return;
            }
            GangMemberEntry oentry = event.getOldEntry(), nentry = event.getEntry();
            if (oentry.isInHideout() != nentry.isInHideout()) {
                updateMembersInHideout();
            }
        }
    };
}
