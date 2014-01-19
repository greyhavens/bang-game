//
// $Id$

package com.threerings.bang.tourney.client;

import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;

import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;

import com.threerings.bang.util.BangContext;

import com.threerings.bang.tourney.data.TourneyListingEntry;

import com.threerings.parlor.tourney.data.Participant;
import com.threerings.parlor.tourney.data.TourneyObject;

import com.threerings.presents.util.SafeSubscriber;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.SetAdapter;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.dobj.Subscriber;
import com.jmex.bui.BContainer;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.BScrollPane;

/**
 * Shows a detailed description of a tourney.
 */
public class TourneyDetailView extends BDecoratedWindow
    implements ActionListener, Subscriber<TourneyObject>
{
    public TourneyDetailView (BangContext ctx, TourneyListingEntry entry)
    {
        super(ctx.getStyleSheet(), entry.desc);

        setModal(true);

        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(BangCodes.TOURNEY_MSGS);

        _safesub = new SafeSubscriber<TourneyObject>(entry.oid, this);

        add(_loading = new BLabel(_msgs.xlate("m.loading")));

    }

    // documentation inherited from interface Subscriber
    public void objectAvailable (TourneyObject tobj)
    {
        _tobj = tobj;
        remove(_loading);

        BContainer cont = new BContainer(new TableLayout(2, 10, 10));
        cont.add(new BLabel(_msgs.xlate("m.min_players"), "right_label"));
        cont.add(new BLabel("" + _tobj.config.minPlayers, "left_label"));
        cont.add(new BLabel(_msgs.xlate("m.starts_in"), "right_label"));
        cont.add(_startsIn = new BLabel("" + _tobj.startsIn, "left_label"));
        cont.add(new BLabel(_msgs.xlate("m.participants"), "right_label"));
        cont.add(_numParticipants = new BLabel("" + _tobj.participants.size(), "left_label"));
        add(cont);

        // setup the participants list
        _partList = GroupLayout.makeVBox(GroupLayout.CENTER);
        BScrollPane scrollPane = new BScrollPane(_partList);
        scrollPane.setPreferredSize(350, 300);
        add(scrollPane);

        for (Participant part : _tobj.participants) {
            _partList.add(new BLabel(part.username.toString()));
        }
        _tobj.addListener(_partlist);
    }

    // documentation inherited from interface Subscriber
    public void requestFailed (int oid, ObjectAccessException cause)
    {
        _loading.setText(_msgs.xlate("m.failed_detail"));
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
    }

    // documentation inherited
    protected void wasAdded ()
    {
        _safesub.subscribe(_ctx.getDObjectManager());
    }

    // documentation inherited
    protected void wasRemoved ()
    {
        _safesub.unsubscribe(_ctx.getDObjectManager());
    }

    /**
     * Show the appropriate buttons for the user.
     */
    protected void recomputeButtons ()
    {
    }

    protected SetListener<Participant> _partlist = new SetAdapter<Participant>() {
        public void entryAdded (EntryAddedEvent<Participant> event) {
            if (event.getName().equals(TourneyObject.PARTICIPANTS)) {
                Participant part = event.getEntry();
                _partList.add(new BLabel(part.username.toString()));
                if (_ctx.getUserObject().handle.equals(part.username)) {
                    recomputeButtons();
                }
            }
        }

        public void entryRemoved (EntryRemovedEvent<Participant> event) {
            if (event.getName().equals(TourneyObject.PARTICIPANTS)) {
                Participant part = event.getOldEntry();
                for (int ii = 0, nn = _partList.getComponentCount(); ii < nn; ii++) {
                    if (((BLabel)_partList.getComponent(ii)).getText().equals(part.username)) {
                        _partList.remove(ii);
                        break;
                    }
                }
                if (_ctx.getUserObject().handle.equals(part.username)) {
                    recomputeButtons();
                }
            }
        }
    };

    protected BLabel _loading;
    protected BLabel _startsIn;
    protected BLabel _numParticipants;
    protected BContainer _partList;
    protected SafeSubscriber<TourneyObject> _safesub;
    protected MessageBundle _msgs;
    protected TourneyObject _tobj;
    protected BangContext _ctx;
}
