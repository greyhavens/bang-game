//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;

import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutObject;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.util.GangUtil;

/**
 * Allows the user to browse the list of gang members.
 */
public class RosterView extends BContainer
    implements AttributeChangeListener, SetListener<GangMemberEntry>, GangCodes, HideoutCodes
{
    public RosterView (
        BangContext ctx, HideoutObject hideoutobj, GangObject gangobj, StatusLabel status)
    {
        super(GroupLayout.makeVStretch());
        _ctx = ctx;
        _hideoutobj = hideoutobj;
        _gangobj = gangobj;
        _status = status;
        _msgs = ctx.getMessageManager().getBundle(HIDEOUT_MSGS);

        setStyleClass("roster_view");

        BContainer rcont = new BContainer(GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        ((GroupLayout)rcont.getLayoutManager()).setGap(-7);

        BContainer tcont = new BContainer(GroupLayout.makeHoriz(
            GroupLayout.STRETCH, GroupLayout.LEFT, GroupLayout.NONE));
        ((GroupLayout)tcont.getLayoutManager()).setOffAxisJustification(GroupLayout.TOP);
        rcont.add(tcont);

        BContainer left = new BContainer(GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        ((GroupLayout)left.getLayoutManager()).setGap(0);
        tcont.add(left);

        left.add(new BLabel(_msgs.get("m.leaders"), "roster_title"));
        left.add(new BLabel(new ImageIcon(_ctx.loadImage("ui/hideout/underline_short.png"))));
        left.add(_lcont = new BContainer(new TableLayout(2)));
        _lcont.setStyleClass("roster_table");

        tcont.add(_lview = new LeaderView(ctx, status), GroupLayout.FIXED);

        BContainer bottom = new BContainer(GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        ((GroupLayout)bottom.getLayoutManager()).setGap(0);
        rcont.add(bottom);

        bottom.add(new BLabel(_msgs.get("m.members"), "roster_title"));
        bottom.add(new BLabel(new ImageIcon(_ctx.loadImage("ui/hideout/underline_long.png"))));
        bottom.add(_mcont = new BContainer(new TableLayout(4)));
        _mcont.setStyleClass("roster_table");

        BScrollPane rpane = new BScrollPane(rcont, true, true);
        rpane.setStyleClass("roster_pane");
        rpane.setShowScrollbarAlways(false);
        add(rpane);
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (event.getName().equals(GangObject.AVATAR)) {
            _lview.update();
        }
    }

    // documentation inherited from interface SetListener
    public void entryAdded (EntryAddedEvent<GangMemberEntry> event)
    {
        if (!event.getName().equals(GangObject.MEMBERS)) {
            return;
        }
        GangMemberEntry entry = event.getEntry();
        updateMembers(entry.rank == LEADER_RANK);
    }

    // documentation inherited from interface SetListener
    public void entryRemoved (EntryRemovedEvent<GangMemberEntry> event)
    {
        if (!event.getName().equals(GangObject.MEMBERS)) {
            return;
        }
        GangMemberEntry entry = event.getOldEntry();
        if (entry.rank == LEADER_RANK) {
            updateMembers(true);
            _lview.update();
        } else {
            updateMembers(false);
        }
    }

    // documentation inherited from interface SetListener
    public void entryUpdated (EntryUpdatedEvent<GangMemberEntry> event)
    {
        if (!event.getName().equals(GangObject.MEMBERS)) {
            return;
        }
        GangMemberEntry oentry = event.getOldEntry(), nentry = event.getEntry();
        if (oentry.rank == LEADER_RANK || nentry.rank == LEADER_RANK) {
            updateMembers(true);
            _lview.update();
        }
        if (oentry.rank != LEADER_RANK || nentry.rank != LEADER_RANK) {
            updateMembers(false);
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        updateMembers();
        _lview.update();
        _gangobj.addListener(this);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _gangobj.removeListener(this);
    }

    protected void updateMembers ()
    {
        updateMembers(true);
        updateMembers(false);
    }

    protected void updateMembers (boolean leaders)
    {
        // add them in sorted order
        BContainer cont = (leaders ? _lcont : _mcont);
        cont.removeAll();
        for (GangMemberEntry entry : GangUtil.getSortedMembers(_gangobj.members, true, leaders)) {
            addMemberEntry(cont, entry);
        }
    }

    protected void addMemberEntry (BContainer cont, GangMemberEntry entry)
    {
        String style = "roster_entry" +
            (entry.isActive() ? (entry.isOnline() ? "" : "_offline") : "_inactive");
        cont.add(new MemberLabel(_ctx, entry, false, _status, style));
        String nstr = (_ctx.getUserObject().gangRank == LEADER_RANK && entry.notoriety > 0) ?
            ("(" + entry.notoriety + ")") : "";
        cont.add(new BLabel(nstr, style));
    }

    protected class LeaderView extends MemberLabel
    {
        public LeaderView (BangContext ctx, StatusLabel status)
        {
            super(ctx, false, status, "leader_view");
            setIcon(_aicon = new AvatarIcon(ctx));
            setIconTextGap(0);
            setOrientation(BLabel.VERTICAL);
        }

        public void update ()
        {
            setMember(_gangobj.getSeniorLeader());
            _aicon.setAvatar(_gangobj.avatar);
        }

        protected AvatarIcon _aicon;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected HideoutObject _hideoutobj;
    protected GangObject _gangobj;
    protected StatusLabel _status;

    protected BContainer _lcont, _mcont;
    protected LeaderView _lview;
}
