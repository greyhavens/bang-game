//
// $Id$

package com.threerings.bang.lobby.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.border.CompoundBorder;
import com.jmex.bui.border.EmptyBorder;
import com.jmex.bui.border.LineBorder;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jme.renderer.ColorRGBA;

import com.threerings.util.MessageBundle;
import com.threerings.util.RandomUtil;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.client.SeatednessObserver;
import com.threerings.parlor.client.TableDirector;
import com.threerings.parlor.client.TableObserver;
import com.threerings.parlor.data.Table;
import com.threerings.parlor.data.TableConfig;
import com.threerings.parlor.data.TableLobbyObject;
import com.threerings.parlor.game.data.GameAI;

import com.threerings.jme.chat.ChatView;

import com.threerings.bang.client.StatusView;
import com.threerings.bang.client.TownView;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.server.scenario.ScenarioFactory;
import com.threerings.bang.lobby.data.LobbyObject;
import com.threerings.bang.util.BangContext;

import java.util.Iterator;

import static com.threerings.bang.Log.log;

/**
 * Displays the interface for a Bang! lobby.
 */
public class LobbyView extends BWindow
    implements PlaceView, TableObserver, SeatednessObserver, ActionListener
{
    public LobbyView (BangContext ctx)
    {
        super(ctx.getLookAndFeel(), new BorderLayout(5, 5));
        _ctx = ctx;

        // display a simple menu when the player presses escape
        setModal(true);
        new StatusView(_ctx).bind(this);

        _chat = new ChatView(_ctx, _ctx.getChatDirector());
        _chat.setBorder(new EmptyBorder(5, 0, 5, 5));
        _chat.setPreferredSize(new Dimension(100, 150));
        add(_chat, BorderLayout.SOUTH);

        MessageBundle msgs =
            ctx.getMessageManager().getBundle("lobby");

        // create our table director
        _tbldtr = new TableDirector(ctx, LobbyObject.TABLE_SET, this);

        // add ourselves as a seatedness observer
        _tbldtr.addSeatednessObserver(this);

        BContainer top = new BContainer(GroupLayout.makeHStretch());
        top.setBorder(new EmptyBorder(5, 5, 5, 0));

        BContainer plist = createLabeledList(msgs.get("m.pending_games"));
        _penders = (BContainer)plist.getComponent(1);
        _penders.setBorder(new CompoundBorder(new LineBorder(ColorRGBA.black),
                                              new EmptyBorder(5, 5, 5, 5)));

        // add our various configuration options
        BContainer blist = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.CENTER));
        blist.add(new BLabel(msgs.get("m.player_count")));
        _seats = new BComboBox(SEATS);
        blist.add(_seats);
        blist.add(new BLabel(msgs.get("m.rounds")));
        _rounds = new BComboBox(ROUNDS);
        blist.add(_rounds);
        blist.add(new BLabel(msgs.get("m.team_size")));
        _tsize = new BComboBox(TEAM_SIZE);
        blist.add(_tsize);

        // configure the controls with the defaults
        BangConfig defconf = new BangConfig();
        _seats.selectItem(Integer.valueOf(defconf.seats));
        _rounds.selectItem(Integer.valueOf(3));
        _tsize.selectItem(Integer.valueOf(defconf.teamSize));

        BButton create = new BButton(msgs.get("m.create"), "create");
        create.addListener(this);
        blist.add(create);
        plist.add(blist, BorderLayout.SOUTH);
        top.add(plist);

        BContainer ilist = createLabeledList(msgs.get("m.in_progress"));
        _inplay = (BContainer)ilist.getComponent(1);
        _inplay.setBorder(new LineBorder(ColorRGBA.black));
        top.add(ilist);
        add(top, BorderLayout.CENTER);
    }

    protected BContainer createLabeledList (String label)
    {
        BContainer outer = new BContainer(new BorderLayout(5, 5));
        outer.add(new BLabel(label), BorderLayout.NORTH);
        GroupLayout vlay = GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH);
        outer.add(new BContainer(vlay), BorderLayout.CENTER);
        return outer;
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();

        setBounds(0, 0, _ctx.getDisplay().getWidth(),
                  _ctx.getDisplay().getHeight());

        // switch to a gray background
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.gray);
    }

    @Override // documentation inherited
    public void wasRemoved ()
    {
        super.wasRemoved();

        // restore the black background
        _ctx.getRenderer().setBackgroundColor(ColorRGBA.black);
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        _lobobj = (LobbyObject)plobj;

        // pass will enter place onto interested parties
        _chat.willEnterPlace(plobj);
        _tbldtr.willEnterPlace(plobj);

        // iterate over the tables already active in this lobby and put
        // them in their respective lists
        TableLobbyObject tlobj = (TableLobbyObject)plobj;
        for (Iterator iter = tlobj.getTables().iterator(); iter.hasNext(); ) {
            tableAdded((Table)iter.next());
        }
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
        // clear out our table lists
        _penders.removeAll();
        _inplay.removeAll();

        _tbldtr.didLeavePlace(plobj);
        _chat.didLeavePlace(plobj);
    }

    // documentation inherited from interface TableObserver
    public void tableAdded (Table table)
    {
        log.info("Table added [table=" + table + "].");

        // create a table item for this table and insert it into the
        // appropriate list
        BContainer host = table.inPlay() ? _inplay : _penders;
        host.add(new TableItem(_ctx, _tbldtr, table));
    }

    // documentation inherited from interface TableObserver
    public void tableUpdated (Table table)
    {
        log.info("Table updated [table=" + table + "].");

        // locate the table item associated with this table
        TableItem item = getTableItem(table.getTableId());
        if (item == null) {
            log.warning("Received table updated notification for " +
                        "unknown table [table=" + table + "].");
            return;
        }

        // let the item perform any updates it finds necessary
        item.tableUpdated(table);

        // and we may need to move the item from the match to the in-play
        // list if it just transitioned
        if (table.gameOid != -1 && item.getParent() == _penders) {
            _penders.remove(item);
            _inplay.add(item);
        }
    }

    // documentation inherited from interface TableObserver
    public void tableRemoved (int tableId)
    {
        log.info("Table removed [tableId=" + tableId + "].");

        // locate the table item associated with this table
        TableItem item = getTableItem(tableId);
        if (item == null) {
            log.warning("Received table removed notification for " +
                        "unknown table [tableId=" + tableId + "].");
            return;
        }

        // remove this item from the user interface
        BContainer parent = (BContainer)item.getParent();
        parent.remove(item);

        // let the little fellow know that we gave him the boot
        item.tableRemoved();
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        TableConfig tconfig = new TableConfig();
        tconfig.desiredPlayerCount = (Integer)_seats.getSelectedItem();
        BangConfig config = new BangConfig();
        // do some temporary jiggery pokery for a one "seat" game; create
        // an AI to play the other opponent
        if (tconfig.desiredPlayerCount == 1) {
            tconfig.desiredPlayerCount = 2;
            config.ais = new GameAI[1];
            config.ais[0] = new GameAI(0, 50);
        }
        config.seats = tconfig.desiredPlayerCount;
        config.scenarios = new String[(Integer)_rounds.getSelectedItem()];
        for (int ii = 0; ii < config.scenarios.length; ii++) {
            config.scenarios[ii] = (String)
                RandomUtil.pickRandom(_lobobj.scenarios);
        }
        config.teamSize = (Integer)_tsize.getSelectedItem();
        _tbldtr.createTable(tconfig, config);
    }

    // documentation inherited from interface SeatednessObserver
    public void seatednessDidChange (boolean isSeated)
    {
//         // update the create table button
//         _create.setEnabled(!isSeated);
    }

    /**
     * Fetches the table item component associated with the specified
     * table id.
     */
    protected TableItem getTableItem (int tableId)
    {
        // first check the pending tables list
        int ccount = _penders.getComponentCount();
        for (int i = 0; i < ccount; i++) {
            TableItem child = (TableItem)_penders.getComponent(i);
            if (child.table.getTableId() == tableId) {
                return child;
            }
        }

        // then the inplay list
        ccount = _inplay.getComponentCount();
        for (int i = 0; i < ccount; i++) {
            TableItem child = (TableItem)_inplay.getComponent(i);
            if (child.table.getTableId() == tableId) {
                return child;
            }
        }

        // sorry charlie
        return null;
    }

    protected BangContext _ctx;
    protected LobbyObject _lobobj;
    protected ChatView _chat;
    protected TableDirector _tbldtr;

    protected BComboBox _seats, _tsize, _rounds;
    protected BContainer _penders;
    protected BContainer _inplay;

    protected static final Integer[] SEATS = new Integer[] {
        Integer.valueOf(1), Integer.valueOf(2),
        Integer.valueOf(3), Integer.valueOf(4) };

    protected static final Integer[] ROUNDS = new Integer[] {
        Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3),
        Integer.valueOf(4), Integer.valueOf(5) };

    protected static final int DEFAULT_ROUNDS_INDEX = 2;

    protected static Integer[] TEAM_SIZE;
    protected static int DEFAULT_TEAM_SIZE_INDEX;
    static {
        TEAM_SIZE = new Integer[GameCodes.MAX_TEAM_SIZE-
                                GameCodes.MIN_TEAM_SIZE+1];
        for (int ii = GameCodes.MIN_TEAM_SIZE;
             ii <= GameCodes.MAX_TEAM_SIZE; ii++) {
            TEAM_SIZE[ii-GameCodes.MIN_TEAM_SIZE] = Integer.valueOf(ii);
        }
        DEFAULT_TEAM_SIZE_INDEX = TEAM_SIZE.length/2;
    };
}
