//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.crowd.client.OccupantAdapter;
import com.threerings.crowd.data.OccupantInfo;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.chat.client.PlaceChatView;

import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;

/**
 * Displays the gang chat, along with a list of gang members in the hideout.
 */
public class GangChatView extends BContainer
    implements HideoutCodes
{
    public GangChatView (
        BangContext ctx, HideoutObject hideoutobj, GangObject gangobj, StatusLabel status)
    {
        super(GroupLayout.makeVert(GroupLayout.TOP));
        _ctx = ctx;
        _hideoutobj = hideoutobj;
        _gangobj = gangobj;
        _status = status;

        BContainer pcont = new BContainer(
            GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP, GroupLayout.NONE));
        pcont.add(new BLabel(_ctx.xlate(HIDEOUT_MSGS, "m.hideout_members"),
            "hideout_members_title"));
        pcont.add(_mcont = new BContainer(new TableLayout(2)));
        BScrollPane spane = new BScrollPane(pcont);
        spane.setStyleClass("hideout_members");
        add(spane);

        add(_pcview = new PlaceChatView(_ctx, _ctx.xlate(HIDEOUT_MSGS, "m.gang_chat")) {
            public boolean displayMessage (ChatMessage msg, boolean alreadyDisplayed) {
                // drop messages from users in other towns
                boolean elsewhere = (msg instanceof UserMessage &&
                    msg.localtype.equals(ChatCodes.PLACE_CHAT_TYPE) &&
                    _ctx.getOccupantDirector().getOccupantInfo(
                        ((UserMessage)msg).speaker) == null);
                return (elsewhere ? false : super.displayMessage(msg, alreadyDisplayed));
            }
        });
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
        _ctx.getOccupantDirector().addOccupantObserver(_occlist);
        _gangobj.addListener(_memberlist);
        updateMembersInHideout();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _ctx.getOccupantDirector().removeOccupantObserver(_occlist);
        _gangobj.removeListener(_memberlist);
    }

    /**
     * Updates the list of members in the hideout.
     */
    protected void updateMembersInHideout ()
    {
        _mcont.removeAll();
        for (OccupantInfo info : _hideoutobj.occupantInfo) {
            GangMemberEntry member = _gangobj.members.get(info.username);
            if (member != null) {
                _mcont.add(new MemberLabel(_ctx, member, true, _status, "hideout_members_entry"));
            }
        }
    }

    protected BangContext _ctx;
    protected HideoutObject _hideoutobj;
    protected GangObject _gangobj;
    protected StatusLabel _status;

    protected BContainer _mcont;
    protected PlaceChatView _pcview;

    /** Listens to the hideout object for changes in occupants. */
    protected OccupantAdapter _occlist = new OccupantAdapter() {
        public void occupantEntered (OccupantInfo info) {
            if (_gangobj.members.containsKey(info.username)) {
                updateMembersInHideout();
            }
        }
        public void occupantLeft (OccupantInfo info) {
            if (_gangobj.members.containsKey(info.username)) {
                updateMembersInHideout();
            }
        }
    };

    /** Listens to the gang object for changes in membership. */
    protected SetAdapter _memberlist = new SetAdapter() {
        public void entryAdded (EntryAddedEvent event) {
            if (!event.getName().equals(GangObject.MEMBERS)) {
                return;
            }
            GangMemberEntry entry = (GangMemberEntry)event.getEntry();
            if (_hideoutobj.getOccupantInfo(entry.handle) != null) {
                updateMembersInHideout();
            }
        }
        public void entryRemoved (EntryRemovedEvent event) {
            if (!event.getName().equals(GangObject.MEMBERS)) {
                return;
            }
            GangMemberEntry entry = (GangMemberEntry)event.getOldEntry();
            if (_hideoutobj.getOccupantInfo(entry.handle) != null) {
                updateMembersInHideout();
            }
        }
    };
}
