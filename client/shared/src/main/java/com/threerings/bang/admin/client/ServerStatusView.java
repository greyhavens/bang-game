//
// $Id$

package com.threerings.bang.admin.client;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;

import com.jmex.bui.BButton;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;
import com.samskivert.util.StringUtil;

import com.threerings.nio.conman.ConMgrStats;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.NamedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangBootstrapData;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.admin.data.StatusObject;

import static com.threerings.bang.Log.log;

/**
 * Displays the status of the running server.
 */
public class ServerStatusView extends BDecoratedWindow
    implements ActionListener, Subscriber<StatusObject>, AttributeChangeListener,
        SetListener<DSet.Entry>
{
    public ServerStatusView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), ctx.xlate("admin", "m.status_title"));

        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle("admin");
        BangBootstrapData bbd = (BangBootstrapData)ctx.getClient().getBootstrapData();
        _safesub = new SafeSubscriber<StatusObject>(bbd.statusOid, this);

        BContainer grid = new BContainer(new TableLayout(2, 5, 25)) {
            protected Dimension computePreferredSize (int whint, int hhint) {
                Dimension d = super.computePreferredSize(whint, hhint);
                _maxWidth = Math.max(d.width, _maxWidth); // never shrink in width
                d.width = _maxWidth;
                return d;
            }
            protected int _maxWidth;
        };
        add(grid, GroupLayout.FIXED);

        // the first row has general stats and conmgr stats
        grid.add(new BLabel(_msgs.get("m.general_stats"), "medium_title"));
        grid.add(new BLabel(_msgs.get("m.conmgr_stats"), "medium_title"), GroupLayout.FIXED);

        BContainer sstats = new BContainer(new TableLayout(2, 5, 15));
        for (int ii = 0; ii < SERVER_STATS.length; ii++) {
            sstats.add(new BLabel(_msgs.get(SERVER_STATS[ii]), "table_label"));
            sstats.add(_genstats[ii] = new BLabel("", "table_data"));
        }
        grid.add(sstats);

        // create our connection manager stats interface
        BContainer cstats = new BContainer(new TableLayout(4, 5, 15));
        for (int ii = 0; ii < CONMGR_STATS.length; ii++) {
            cstats.add(new BLabel(_msgs.get(CONMGR_STATS[ii]), "table_label"));
            cstats.add(_constats[ii] = new BLabel("", "table_data"));
        }
        grid.add(cstats);

        grid.add(new BLabel(_msgs.get("m.place_stats"), "medium_title"));
        grid.add(new BLabel(_msgs.get("m.game_stats"), "medium_title"));

        _placebox = new BContainer(new TableLayout(4, 5, 15));
        grid.add(_placebox);

        BContainer gstats = new BContainer(new TableLayout(6, 5, 15));
        _gamestats = new BLabel[3 + 2 * EnumSet.allOf(StatusObject.GameType.class).size()];
        gstats.add(new BLabel(_msgs.get("m.matches_pending"), "table_label"));
        gstats.add(_gamestats[0] = new BLabel("0", "table_data"));
        gstats.add(new BLabel("", "table_data"));
        gstats.add(new BLabel(_msgs.get("m.active_games"), "table_label"));
        gstats.add(_gamestats[1] = new BLabel("0", "table_data"));
        gstats.add(_gamestats[2] = new BLabel("0", "table_data"));
        for (StatusObject.GameType type : StatusObject.GameType.values()) {
            String ident = type.toString().toLowerCase();
            gstats.add(new BLabel(_msgs.get("m.type_" + ident), "table_label"));
            int pos = type.ordinal() * 2;
            gstats.add(_gamestats[3 + pos] = new BLabel("0", "table_data"));
            gstats.add(_gamestats[4 + pos] = new BLabel("0", "table_data"));
        }
        grid.add(gstats);

        add(new Spacer(1, 1));

        // create an interface for sending a broadcast message
        add(new BLabel(_msgs.get("m.send_broadcast"), "medium_title"), GroupLayout.FIXED);
        BContainer bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout)bcont.getLayoutManager()).setOffAxisJustification(GroupLayout.TOP);
        bcont.add(new BLabel(_msgs.get("m.bcast_text"), "table_label"));
        bcont.add(_bcast = new BTextField(""));
        _bcast.setPreferredWidth(400);
        _bcast.addListener(this);
        bcont.add(new BButton(_msgs.get("m.send"), this, "send_bcast"));
        add(bcont, GroupLayout.FIXED);

        add(new Spacer(1, 1));

        // create an interface for scheduling a reboot
        add(new BLabel(_msgs.get("m.schedule_reboot"), "medium_title"), GroupLayout.FIXED);
        bcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        bcont.add(new BLabel(_msgs.get("m.reboot_for"), "table_label"));
        RebootOption[] opts = new RebootOption[REBOOT_MINS.length];
        for (int ii = 0; ii < opts.length; ii++) {
            opts[ii] = new RebootOption(REBOOT_MINS[ii]);
        }
        bcont.add(_rebmins = new BComboBox(opts));
        _rebmins.selectItem(REBOOT_MINS.length-2);
        bcont.add(new BButton(_msgs.get("m.schedule"), this, "sched_reboot"));

        bcont.add(new Spacer(25, 1));
        bcont.add(new BLabel(_msgs.get("m.next_reboot"), "table_label"));
        bcont.add(_nextReboot = new BLabel("", "table_data"));
        add(bcont, GroupLayout.FIXED);

        add(new Spacer(1, 1));

        // add a dismiss button for non-keyboard lovers
        add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"), GroupLayout.FIXED);
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if ("dismiss".equals(cmd)) {
            _ctx.getBangClient().clearPopup(this, true);

        } else if ("send_bcast".equals(cmd) || event.getSource() == _bcast) {
            String bcast = _bcast.getText();
            if (!StringUtil.isBlank(bcast)) {
                _ctx.getChatDirector().requestBroadcast(bcast);
                _bcast.setText("");
            }

        } else if ("sched_reboot".equals(cmd)) {
            _statobj.service.scheduleReboot(((RebootOption)_rebmins.getSelectedItem()).minutes);
        }
    }

    // from interface Subscriber
    public void objectAvailable (StatusObject object)
    {
        _statobj = object;
        _statobj.addListener(this);

        // we had to wait until we got the object to create our place stats table
        for (StatusObject.PlaceInfo info : _statobj.places) {
            String label = _ctx.xlate(BangCodes.BANG_MSGS, "m." + info.ident);
            _placebox.add(new BLabel(label, "table_label"));
            BLabel pcount = new BLabel("", "table_data");
            _placebox.add(pcount);
            _placestats.put(info.ident, pcount);
        }

        // update the UI
        updateGeneralStats();
        updateConMgrStats();
        updatePlaceStats();
        updateGameStats();
        updateReboot();
    }

    // from interface Subscriber
    public void requestFailed (int oid, ObjectAccessException cause)
    {
        log.warning("Failed to subscribe to status object: " + cause + ".");
    }

    // from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        updateStats(event);
    }

    // from interface SetListener
    public void entryAdded (EntryAddedEvent<DSet.Entry> event)
    {
        updateStats(event);
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent<DSet.Entry> event)
    {
        updateStats(event);
    }

    // from interface SetListener
    public void entryRemoved (EntryRemovedEvent<DSet.Entry> event)
    {
        updateStats(event);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _safesub.subscribe(_ctx.getClient().getDObjectManager());
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        if (_statobj != null) {
            _statobj.removeListener(this);
            _statobj = null;
        }
        _safesub.unsubscribe(_ctx.getClient().getDObjectManager());
    }

    @Override
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        return new Dimension(800, 600);
    }

    protected void updateStats (NamedEvent event)
    {
        if (event.getName().equals(StatusObject.CONN_STATS)) {
            updateConMgrStats();
        } else if (event.getName().equals(StatusObject.SERVER_REBOOT_TIME)) {
            updateReboot();
        } else if (event.getName().equals(StatusObject.PLAYERS_ONLINE)) {
            updateGeneralStats();
        } else if (event.getName().equals(StatusObject.PENDING_MATCHES)) {
            updateGameStats();
        } else if (event.getName().equals(StatusObject.PLACES)) {
            updatePlaceStats();
        } else if (event.getName().equals(StatusObject.GAMES)) {
            updateGameStats();
        }
    }

    protected void updateGeneralStats ()
    {
        _genstats[0].setText(_ssfmt.format(new Date(_statobj.serverStartTime)));
        _genstats[1].setText(String.valueOf(_statobj.playersOnline));
    }

    protected void updateGameStats ()
    {
        _gamestats[0].setText(String.valueOf(_statobj.pendingMatches));
        _gamestats[1].setText(String.valueOf(_statobj.games.size()));

        HashMap<StatusObject.GameType, int[]> tcounts = new HashMap<StatusObject.GameType, int[]>();
        for (StatusObject.GameType type : StatusObject.GameType.values()) {
            tcounts.put(type, new int[2]);
        }
        int playersInGames = 0;
        for (StatusObject.GameInfo gi : _statobj.games) {
            playersInGames += gi.players;
            int[] counts = tcounts.get(gi.type);
            counts[0]++;
            counts[1] += gi.players;
        }
        _gamestats[2].setText(String.valueOf(playersInGames));

        for (StatusObject.GameType type : StatusObject.GameType.values()) {
            int[] counts = tcounts.get(type);
            _gamestats[3 + type.ordinal()*2].setText(String.valueOf(counts[0]));
            _gamestats[4 + type.ordinal()*2].setText(String.valueOf(counts[1]));
        }
    }

    protected void updatePlaceStats ()
    {
        for (StatusObject.PlaceInfo info : _statobj.places) {
            BLabel pcount = _placestats.get(info.ident);
            pcount.setText(String.valueOf(info.occupants));
        }
    }

    protected void updateConMgrStats ()
    {
        ConMgrStats cms = _statobj.connStats;
        // _constats[0].setText(String.valueOf(cms.authQueueSize));
        _constats[1].setText(String.valueOf(cms.bytesIn));
        _constats[2].setText(String.valueOf(cms.deathQueueSize));
        _constats[3].setText(String.valueOf(cms.bytesOut));
        _constats[4].setText(String.valueOf(cms.outQueueSize));
        _constats[5].setText(String.valueOf(cms.msgsIn));
        _constats[6].setText(String.valueOf(cms.overQueueSize));
        _constats[7].setText(String.valueOf(cms.msgsOut));
    }

    protected void updateReboot ()
    {
        _nextReboot.setText(_statobj.serverRebootTime == 0L ?
                            _msgs.get("m.reboot_never") :
                            _ssfmt.format(new Date(_statobj.serverRebootTime)));
    }

    protected class RebootOption
    {
        public int minutes;
        public RebootOption (int minutes) {
            this.minutes = minutes;
        }
        public String toString () {
            return _msgs.get("m.minute", minutes);
        }
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected SafeSubscriber<StatusObject> _safesub;
    protected StatusObject _statobj;

    protected BLabel[] _genstats = new BLabel[SERVER_STATS.length];
    protected BLabel[] _constats = new BLabel[CONMGR_STATS.length];
    protected BLabel[] _gamestats;

    protected BContainer _placebox;
    protected HashMap<String,BLabel> _placestats = new HashMap<String,BLabel>();

    protected BTextField _bcast;
    protected BComboBox _rebmins;
    protected BLabel _nextReboot;

    protected SimpleDateFormat _ssfmt = new SimpleDateFormat("EEE MMM, dd hh:mm aa");

    protected static final String[] SERVER_STATS = {
        "m.server_started", "m.players_online",
    };

    protected static final String[] CONMGR_STATS = {
        "m.authq",  "m.bytesin",
        "m.deathq", "m.bytesout",
        "m.outq",   "m.msgsin",
        "m.overq",  "m.msgsout"
    };

    protected static final int[] REBOOT_MINS = { 0, 2, 5, 15, 30, 60 };
}
