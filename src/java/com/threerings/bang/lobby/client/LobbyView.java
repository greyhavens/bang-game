//
// $Id$

package com.threerings.bang.lobby.client;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;

import com.jme.renderer.ColorRGBA;
import com.jmex.bui.BButton;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTextField;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

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

import com.threerings.bang.client.BangClient;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.lobby.data.LobbyObject;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Displays the interface for a Bang! lobby.
 */
public class LobbyView extends BWindow
    implements PlaceView, TableObserver, SeatednessObserver, ActionListener
{
    public LobbyView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), new BorderLayout(5, 5));
        setStyleClass("main_view");

        _ctx = ctx;

        _chat = new ChatView(_ctx, _ctx.getChatDirector());
        _chat.setStyleClass("lobby_chat");
        _chat.setPreferredSize(new Dimension(100, 150));
        add(_chat, BorderLayout.SOUTH);

        MessageBundle msgs =
            ctx.getMessageManager().getBundle("lobby");

        // create our table director
        _tbldtr = new TableDirector(ctx, LobbyObject.TABLE_SET, this);

        // add ourselves as a seatedness observer
        _tbldtr.addSeatednessObserver(this);

        BContainer top = new BContainer(GroupLayout.makeHStretch());

        BContainer plist = createLabeledList(msgs.get("m.pending_games"));
        _penders = (BContainer)plist.getComponent(1);
        _penders.setStyleClass("padded_box");

        // add our various configuration options
        TableLayout tlay = new TableLayout(5, 5, 5);
        tlay.setHorizontalAlignment(TableLayout.CENTER);
        BContainer blist = new BContainer(tlay);
        blist.add(new BLabel(msgs.get("m.player_count")));
        _seats = new BComboBox(SEATS);
        blist.add(_seats);

        blist.add(new BLabel(msgs.get("m.scenario")));
        _scenarios = new BComboBox(
            new Object[] { new ScenarioLabel("random") });
        blist.add(_scenarios);

        blist.add(_board = new BTextField());
        _board.setPreferredWidth(100);

        blist.add(new BLabel(msgs.get("m.rounds")));
        _rounds = new BComboBox(ROUNDS);
        blist.add(_rounds);

        blist.add(new BLabel(msgs.get("m.team_size")));
        _tsize = new BComboBox(TEAM_SIZE);
        blist.add(_tsize);

        // configure the controls with the defaults
        _seats.selectItem(Integer.valueOf(2));
        _rounds.selectItem(Integer.valueOf(3));
        _tsize.selectItem(DEFAULT_TEAM_SIZE_INDEX);
        _scenarios.selectItem(0);

        _create = new BButton(msgs.get("m.create"), "create");
        _create.addListener(this);
        blist.add(_create);
        plist.add(blist, BorderLayout.SOUTH);
        top.add(plist);

        BContainer ilist = createLabeledList(msgs.get("m.in_progress"));
        _inplay = (BContainer)ilist.getComponent(1);
        _inplay.setStyleClass("padded_box");
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
        _tbldtr.setTableObject(plobj);

        // iterate over the tables already active in this lobby and put
        // them in their respective lists
        TableLobbyObject tlobj = (TableLobbyObject)plobj;
        for (Table table : tlobj.getTables()) {
            tableAdded(table);
        }

        // add our scenarios to the drop down
        for (int ii = 0; ii < _lobobj.scenarios.length; ii++) {
            _scenarios.addItem(new ScenarioLabel(_lobobj.scenarios[ii]));
        }
    }

    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
        // clear out our table lists
        _penders.removeAll();
        _inplay.removeAll();

        _tbldtr.clearTableObject();
        _chat.didLeavePlace(plobj);
    }

    // documentation inherited from interface TableObserver
    public void tableAdded (Table table)
    {
        log.info("Table added", "table", table);

        // create a table item for this table and insert it into the
        // appropriate list
        BContainer host = table.inPlay() ? _inplay : _penders;
        host.add(new TableItem(_ctx, _tbldtr, table));
    }

    // documentation inherited from interface TableObserver
    public void tableUpdated (Table table)
    {
        log.info("Table updated", "table", table);

        // locate the table item associated with this table
        TableItem item = getTableItem(table.tableId);
        if (item == null) {
            log.warning("Received table updated notification for unknown table", "table", table);
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
        log.info("Table removed", "tableId", tableId);

        // locate the table item associated with this table
        TableItem item = getTableItem(tableId);
        if (item == null) {
            log.warning("Received table removed notification for unknown table",
                        "tableId", tableId);
            return;
        }

        // remove this item from the user interface
        BContainer parent = item.getParent();
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
        config.init(tconfig.desiredPlayerCount, (Integer)_tsize.getSelectedItem());

        // if they specified the name of a board file, try using that
        String bname = _board.getText();
        byte[] bdata = null;
        if (bname.endsWith(".board")) {
            String error = null;
            File board = new File(
                BangClient.localDataDir("rsrc" + File.separator + "boards" + File.separator +
                                        String.valueOf(tconfig.desiredPlayerCount)), bname);
            if (!board.exists()) {
                error = "File not found.";
            }
            try {
                bdata = IOUtils.toByteArray(new FileInputStream(board));
            } catch (Exception e) {
                error = e.getMessage();
            }
            if (error != null) {
                String msg = MessageBundle.tcompose("m.board_load_failed", board.getPath(), error);
                _ctx.getChatDirector().displayFeedback("lobby", msg);
                return;
            }

        } else if (StringUtil.isBlank(bname)) {
            bname = null;
        }

        // now configure the rounds
        String id = ((ScenarioLabel)_scenarios.getSelectedItem()).id;
        for (int ii = 0; ii < (Integer)_rounds.getSelectedItem(); ii++) {
            String scen = id.equals("random") ? RandomUtil.pickRandom(_lobobj.scenarios) : id;
            config.addRound(scen, bname, bdata);
        }

        _tbldtr.createTable(tconfig, config);
    }

    // documentation inherited from interface SeatednessObserver
    public void seatednessDidChange (boolean isSeated)
    {
        // update the create table button
        _create.setEnabled(!isSeated);
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
            if (child.table.tableId == tableId) {
                return child;
            }
        }

        // then the inplay list
        ccount = _inplay.getComponentCount();
        for (int i = 0; i < ccount; i++) {
            TableItem child = (TableItem)_inplay.getComponent(i);
            if (child.table.tableId == tableId) {
                return child;
            }
        }

        // sorry charlie
        return null;
    }

    /** Used to display selectable scenarios. */
    protected class ScenarioLabel
    {
        public String id;
        public ScenarioLabel (String id) {
            this.id = id;
        }
        public String toString () {
            return _ctx.xlate(GameCodes.GAME_MSGS, "m.scenario_" + id);
        }
    }

    protected BangContext _ctx;
    protected LobbyObject _lobobj;
    protected ChatView _chat;
    protected TableDirector _tbldtr;

    protected BButton _create;
    protected BComboBox _seats, _tsize, _rounds, _scenarios;
    protected BContainer _penders;
    protected BContainer _inplay;
    protected BTextField _board;

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
