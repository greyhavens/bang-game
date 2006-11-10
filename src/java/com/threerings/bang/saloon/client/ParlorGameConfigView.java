//
// $Id$

package com.threerings.bang.saloon.client;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import org.apache.commons.io.IOUtils;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.samskivert.util.CollectionUtil;
import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.client.BangClient;
import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.client.util.ReportingListener;
import com.threerings.bang.client.util.StateSaver;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.scenario.ScenarioInfo;

import com.threerings.bang.saloon.data.ParlorGameConfig;
import com.threerings.bang.saloon.data.ParlorObject;
import com.threerings.bang.saloon.data.SaloonCodes;

/**
 * Handles the interface for configuring a game in a Back Parlor.
 */
public class ParlorGameConfigView extends BContainer
    implements AttributeChangeListener, ActionListener
{
    public ParlorGameConfigView (BangContext ctx, StatusLabel status)
    {
        super(new BorderLayout(0, 10));

        _ctx = ctx;
        _status = status;
        _msgs = ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);

        BContainer main = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.NONE, GroupLayout.CENTER,
                                  GroupLayout.CONSTRAIN));
        ((GroupLayout)main.getLayoutManager()).setGap(25);
        add(main, BorderLayout.CENTER);

        // create our various combo boxen
        BContainer combos = new BContainer(new TableLayout(2, 5, 5));
        main.add(combos);

        // load our slot icons
        _psicons = new ImageIcon[] {
            new ImageIcon(_ctx.loadImage("ui/saloon/pardners_only.png")),
            new ImageIcon(_ctx.loadImage("ui/saloon/anyone.png")),
            new ImageIcon(_ctx.loadImage("ui/saloon/locked.png"))
        };

        // number of players
        combos.add(BangUI.createLabel(_msgs, "m.par_players", "match_label"));
        BContainer pslots = GroupLayout.makeHBox(GroupLayout.CENTER);
        _slots = new PlayerSlotButton[GameCodes.MAX_PLAYERS];
        for (int ii = 0; ii < _slots.length; ii++) {
            int opt = (ii > 0) ? ((ii > 1) ? 3 : 2) : 1;
            pslots.add(_slots[ii] = new PlayerSlotButton(opt));
        }
        combos.add(pslots);
        setSlotStates(BangPrefs.config.getValue("parlor.players", 2),
                      BangPrefs.config.getValue("parlor.tincans", 1));

        // rounds of play
        combos.add(BangUI.createLabel(_msgs, "m.rounds", "match_label"));
        combos.add(
            _rounds = new BComboBox(makeBoxItems(1, GameCodes.MAX_ROUNDS)));
        _rounds.selectItem(1);
        _rounds.addListener(this);
        new StateSaver("parlor.rounds", _rounds);

        // units per team
        combos.add(BangUI.createLabel(_msgs, "m.units", "match_label"));
        combos.add(
            _units = new BComboBox(makeBoxItems(1, GameCodes.MAX_TEAM_SIZE)));
        _units.selectItem(2);
        _units.addListener(this);
        new StateSaver("parlor.units", _units);

        // game duration
        combos.add(BangUI.createLabel(_msgs, "m.duration", "match_label"));
        BComboBox.Item[] ditems = new BComboBox.Item[] {
            new BComboBox.Item(BangConfig.Duration.QUICK,
                               _msgs.get("m.dur_quick")),
            new BComboBox.Item(BangConfig.Duration.NORMAL,
                               _msgs.get("m.dur_normal")),
            new BComboBox.Item(BangConfig.Duration.LONG,
                               _msgs.get("m.dur_long"))
        };
        combos.add(_duration = new BComboBox(ditems));
        _duration.selectItem(1);
        _duration.addListener(this);
        new StateSaver("parlor.duration", _duration);

        // tick speed
        combos.add(BangUI.createLabel(_msgs, "m.speed", "match_label"));
        BComboBox.Item[] sitems = new BComboBox.Item[] {
            new BComboBox.Item(BangConfig.Speed.FAST,
                               _msgs.get("m.sp_fast")),
            new BComboBox.Item(BangConfig.Speed.NORMAL,
                               _msgs.get("m.sp_normal")),
            new BComboBox.Item(BangConfig.Speed.SLOW,
                               _msgs.get("m.sp_slow"))
        };
        combos.add(_speed = new BComboBox(sitems));
        _speed.selectItem(1);
        _speed.addListener(this);
        new StateSaver("parlor.speed", _speed);

        // create our scenario toggles
        BContainer scenbox = new BContainer(new BorderLayout());
        scenbox.setStyleClass("parlor_scenbox");
        scenbox.add(BangUI.createLabel(_msgs, "m.scenarios", "match_header"),
                    BorderLayout.NORTH);
        BContainer checkboxen = new BContainer(
            GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                                 GroupLayout.EQUALIZE));
        _scenIds = ScenarioInfo.getScenarioIds(
            _ctx.getUserObject().townId, true);
        _scens = new BCheckBox[_scenIds.length];
        for (int ii = 0; ii < _scens.length; ii++) {
            _scens[ii] = new BCheckBox(
                _ctx.xlate(GameCodes.GAME_MSGS, "m.scenario_" + _scenIds[ii]));
            _scens[ii].setSelected(true);
            _scens[ii].addListener(this);
            checkboxen.add(_scens[ii]);
            new StateSaver("parlor.scenario." + _scenIds[ii], _scens[ii]);
        }
        BScrollPane pane = new BScrollPane(checkboxen);
        pane.setViewportStyleClass("parlor_scenarios");
        ((BorderLayout)pane.getLayoutManager()).setGaps(3, 5);
        scenbox.add(pane, BorderLayout.CENTER);
        if (_ctx.getUserObject().tokens.isAdmin()) {
            scenbox.add(_board = new BTextField(""), BorderLayout.SOUTH);
        }
        main.add(scenbox);

        // create our "Create" button
        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        _create = new BButton(_msgs.get("m.create"), this, "create");
        _create.setStyleClass("big_button");
        buttons.add(_create);
        add(buttons, BorderLayout.SOUTH);
    }

    public void willEnterPlace (ParlorObject parobj)
    {
        _parobj = parobj;
        _parobj.addListener(this);
        updateDisplay();
    }

    public void didLeavePlace ()
    {
        if (_parobj != null) {
            _parobj.removeListener(this);
            _parobj = null;
        }
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (ParlorObject.GAME.equals(event.getName()) ||
            ParlorObject.ONLY_CREATOR_START.equals(event.getName())) {
            updateDisplay();
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if (event.getSource() == _create) {
            startMatchMaking();
        } else {
            if (shouldSyncGameConfig()) {
                _parobj.service.updateGameConfig(
                    _ctx.getClient(), makeConfig());
            }
        }
    }

    protected boolean shouldSyncGameConfig ()
    {
        if (_updatingDisplay) {
            return false;
        }
        if (_parobj.game == null) {
            return true;
        }
        if ((Integer)_rounds.getSelectedItem() != _parobj.game.rounds ||
            (Integer)_units.getSelectedItem() != _parobj.game.teamSize ||
            getSlotCount(HUMAN) != _parobj.game.players ||
            getSlotCount(TINCAN) != _parobj.game.tinCans) {
            return true;
        }

        HashSet<String> set = new HashSet<String>();
        CollectionUtil.addAll(set, _parobj.game.scenarios);
        for (int ii = 0; ii < _scenIds.length; ii++) {
            if (_scens[ii].isSelected() != set.contains(_scenIds[ii])) {
                return true;
            }
        }
        return false;
    }

    protected void updateDisplay ()
    {
        _updatingDisplay = true;
        if (_parobj.game != null) {
            _rounds.selectItem(Integer.valueOf(_parobj.game.rounds));
            _units.selectItem(Integer.valueOf(_parobj.game.teamSize));
            setSlotStates(_parobj.game.players, _parobj.game.tinCans);
            HashSet<String> set = new HashSet<String>();
            CollectionUtil.addAll(set, _parobj.game.scenarios);
            for (int ii = 0; ii < _scenIds.length; ii++) {
                _scens[ii].setSelected(set.contains(_scenIds[ii]));
            }
        }

        boolean canCreate =
            _ctx.getUserObject().handle.equals(_parobj.info.creator) ||
            !_parobj.onlyCreatorStart;
        _rounds.setEnabled(canCreate);
        _units.setEnabled(canCreate);
        _duration.setEnabled(canCreate);
        _speed.setEnabled(canCreate);
        for (int ii = 0; ii < _scens.length; ii++) {
            _scens[ii].setEnabled(canCreate);
        }
        _create.setEnabled(canCreate);
        _updatingDisplay = false;
    }

    protected ParlorGameConfig makeConfig ()
    {
        ParlorGameConfig config = new ParlorGameConfig();
        config.rounds = (Integer)_rounds.getSelectedItem();
        config.teamSize = (Integer)_units.getSelectedItem();
        config.duration = (BangConfig.Duration)_duration.getSelectedValue();
        config.speed = (BangConfig.Speed)_speed.getSelectedValue();
        config.players = getSlotCount(HUMAN);
        config.tinCans = getSlotCount(TINCAN);

        // store these to preferences because we don't track them magically
        BangPrefs.config.setValue("parlor.players", config.players);
        BangPrefs.config.setValue("parlor.tincans", config.tinCans);

        ArrayList<String> scens = new ArrayList<String>();
        for (int ii = 0; ii < _scenIds.length; ii++) {
            if (_scens[ii].isSelected()) {
                scens.add(_scenIds[ii]);
            }
        }
        config.scenarios = scens.toArray(new String[scens.size()]);
        return config;
    }

    protected void startMatchMaking ()
    {
        // create the parlor game config
        ParlorGameConfig config = makeConfig();

        // if there's a custom board involved, load that
        byte[] bdata = null;
        String bfile = (_board == null) ? null : _board.getText();
        if (!StringUtil.isBlank(bfile)) {
            String error = null;
            int pcount = config.players + config.tinCans;
            File board = new File(
                BangClient.localDataDir(
                    "rsrc" + File.separator + "boards" + File.separator +
                    String.valueOf(pcount)), bfile);
            if (!board.exists()) {
                error = "m.board_not_found";
            }
            try {
                bdata = IOUtils.toByteArray(new FileInputStream(board));
            } catch (Exception e) {
                error = MessageBundle.taint(e.getMessage());
            }
            if (error != null) {
                String msg = MessageBundle.taint(board.getPath());
                msg = MessageBundle.compose("m.board_load_failed", msg, error);
                msg = _ctx.xlate(SaloonCodes.SALOON_MSGS, msg);
                _status.setStatus(msg, true);
                return;
            }
        }

        // finally start things up
        ReportingListener rl = new ReportingListener(
            _ctx, SaloonCodes.SALOON_MSGS, "m.create_game_failed");
        _parobj.service.startMatchMaking(
            _ctx.getClient(), config, bdata, rl);
    }

    protected Object[] makeBoxItems (int low, int high)
    {
        ArrayList<Integer> items = new ArrayList<Integer>();
        for (int ii = low; ii <= high; ii++) {
            items.add(ii);
        }
        return items.toArray(new Integer[items.size()]);
    }

    protected void setSlotStates (int players, int tinCans)
    {
        for (int ii = 0; ii < _slots.length; ii++) {
            if (players > ii) {
                _slots[ii].setState(HUMAN);
            } else if (players + tinCans > ii) {
                _slots[ii].setState(TINCAN);
            } else {
                _slots[ii].setState(NONE);
            }
        }
    }

    protected int getSlotCount (int type)
    {
        int count = 0;
        for (int ii = 0; ii < _slots.length; ii++) {
            if (_slots[ii].getState() == type) {
                count++;
            }
        }
        return count;
    }

    protected class PlayerSlotButton extends BLabel
    {
        public PlayerSlotButton (int choices) {
            super((String)null);
            _icons = new ImageIcon[choices];
            System.arraycopy(_psicons, 0, _icons, 0, choices);
            setIcon(_icons[_state]);
        }

        public int getState () {
            return _state;
        }

        public void setState (int state) {
            if (_state != state) {
                setIcon(_icons[_state = state]);
            }
        }

        @Override
        public boolean dispatchEvent (BEvent event) {
            if (isEnabled() && event instanceof MouseEvent) {
                MouseEvent mev = (MouseEvent)event;
                if (mev.getType() == MouseEvent.MOUSE_PRESSED) {
                    // toggle to the next state
                    setState((_state + 1) % _icons.length);
                    return true;
                } else {
                    return super.dispatchEvent(event);
                }
            }
            return super.dispatchEvent(event);
        }

        @Override
        public String getTooltipText () {
            return _msgs.get("m.slot_tip" + _state);
        }

        protected ImageIcon[] _icons;
        protected int _state;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected ParlorObject _parobj;
    protected StatusLabel _status;

    protected BComboBox _rounds, _units;
    protected BComboBox _duration, _speed;

    protected ImageIcon[] _psicons;
    protected PlayerSlotButton[] _slots;

    protected String[] _scenIds;
    protected BCheckBox[] _scens;
    protected BButton _create;

    protected boolean _updatingDisplay = false;

    // only usable by admins for now
    protected BTextField _board;

    protected static final int HUMAN = 0;
    protected static final int TINCAN = 1;
    protected static final int NONE = 2;
}
