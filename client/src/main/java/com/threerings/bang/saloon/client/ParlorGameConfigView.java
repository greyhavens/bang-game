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
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BTextField;
import com.jmex.bui.background.ImageBackground;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.icon.SubimageIcon;
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
import com.threerings.bang.saloon.data.ParlorGameConfig.Slot;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.TableGameObject;

/**
 * Handles the interface for configuring a game in a Back Parlor.
 */
public class ParlorGameConfigView extends BContainer
    implements AttributeChangeListener, ActionListener
{
    public ParlorGameConfigView (BangContext ctx, StatusLabel status, TableGameView tview)
    {
        super(new BorderLayout(0, 10));

        _ctx = ctx;
        _status = status;
        _tview = tview;
        _msgs = ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);

        BContainer main = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.NONE, GroupLayout.CENTER,
                                  GroupLayout.CONSTRAIN).setGap(25));
        add(main, BorderLayout.CENTER);

        // create our various combo boxen
        BContainer combos = new BContainer(new TableLayout(2, 5, 5));
        main.add(combos);

        // load our slot icons
        BImage icons = _ctx.loadImage("ui/saloon/player_types.png");
        int width = icons.getWidth()/3, height = icons.getHeight();
        _psicons = new SubimageIcon[] {
            new SubimageIcon(icons, 0, 0, width, height),
            new SubimageIcon(icons, width, 0, width, height),
            new SubimageIcon(icons, 2*width, 0, width, height),
        };

        // load our slot backgrounds
        _slotbgs = new BImage[3];
        _slothbgs = new BImage[3];
        for (int ii = 0; ii < 3; ii++) {
            _slotbgs[ii] = _ctx.loadImage("ui/buttons/n_state_normal" + ii + ".png");
            _slothbgs[ii] = _ctx.loadImage("ui/buttons/n_state_hover" + ii + ".png");
        }

        // number of players
        combos.add(BangUI.createLabel(_msgs, "m.par_players", "match_label"));
        BContainer pslots = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.CENTER).setGap(2));
        _slots = new PlayerSlotButton[GameCodes.MAX_PLAYERS];
        Slot[] slotLookup = Slot.values();
        for (int ii = 0; ii < _slots.length; ii++) {
            Slot max = slotLookup[Math.min(ii, 2)];
            pslots.add(_slots[ii] = new PlayerSlotButton(max));
            _slots[ii].setType(slotLookup[BangPrefs.config.getValue("parlor.slot." + ii, 0)]);
            _slots[ii].addListener(this);
        }
        combos.add(pslots);

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

        BContainer scenbox = new BContainer(new BorderLayout(0, 5));
        scenbox.setStyleClass("parlor_scenbox");

        BContainer modecont = new BContainer(GroupLayout.makeHoriz(
                    GroupLayout.STRETCH, GroupLayout.CENTER, GroupLayout.NONE).setGap(5));
        modecont.add(BangUI.createLabel(_msgs, "m.mode", "match_label"), GroupLayout.FIXED);
        BComboBox.Item[] mitems = new BComboBox.Item[] {
            new BComboBox.Item(ParlorGameConfig.Mode.NORMAL, _msgs.get("m.mode_normal")),
            new BComboBox.Item(ParlorGameConfig.Mode.TEAM_2V2, _msgs.get("m.mode_2v2"))
        };
        modecont.add(_mode = new BComboBox(mitems));
        _mode.selectItem(0);
        _mode.addListener(this);
        new StateSaver("parlor.mode", _mode);
        scenbox.add(modecont, BorderLayout.NORTH);

        // create our scenario toggles
        //scenbox.add(BangUI.createLabel(_msgs, "m.scenarios", "match_header"),
        //            BorderLayout.NORTH);
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
        _buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        _create = new BButton(_msgs.get("m.create"), this, "create");
        _create.setStyleClass("big_button");
        _ownerIcon = new BLabel(new ImageIcon(_ctx.loadImage("ui/saloon/only_parlor_owner.png")));
        _buttons.add(_create);
        add(_buttons, BorderLayout.SOUTH);
    }

    public void willEnterPlace (TableGameObject tobj)
    {
        _tobj = tobj;
        _tobj.addListener(this);
    }

    public void didLeavePlace ()
    {
        // no trying to create matches on our way out
        _create.setEnabled(false);
        if (_tobj != null) {
            _tobj.removeListener(this);
            _tobj = null;
        }
    }

    // documentation inherited from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (TableGameObject.GAME.equals(event.getName())) {
            updateDisplay();
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if (event.getSource() == _create) {
            // make sure we're still in the parlor
            if (_tobj != null) {
                startMatchMaking();
            }
        } else {
            if (event.getSource() == _mode) {
                modeUpdated();
            }
            if (shouldSyncGameConfig()) {
                _tobj.service.updateGameConfig(makeConfig());
            }
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        modeUpdated();
    }

    protected boolean shouldSyncGameConfig ()
    {
        if (_updatingDisplay) {
            return false;
        }
        if (_tobj.game == null) {
            return true;
        }
        if ((Integer)_rounds.getSelectedItem() != _tobj.game.rounds ||
            (Integer)_units.getSelectedItem() != _tobj.game.teamSize ||
            (ParlorGameConfig.Mode)_mode.getSelectedValue() != _tobj.game.mode ||
            (BangConfig.Duration)_duration.getSelectedValue() != _tobj.game.duration ||
            (BangConfig.Speed)_speed.getSelectedValue() != _tobj.game.speed ||
            slotsChanged(_tobj.game.slots)) {
            return true;
        }

        HashSet<String> set = new HashSet<String>();
        CollectionUtil.addAll(set, _tobj.game.scenarios);
        for (int ii = 0; ii < _scenIds.length; ii++) {
            if (_scens[ii].isSelected() != set.contains(_scenIds[ii])) {
                return true;
            }
        }
        return false;
    }

    protected void modeUpdated ()
    {
        // make sure we're not in the middle of updating ourselves
        if (_updatingDisplay) {
            return;
        }
        _updatingDisplay = true;
        switch((ParlorGameConfig.Mode)_mode.getSelectedValue()) {
        case NORMAL:
            for (int ii = 0; ii < _slots.length; ii++) {
                _slots[ii].setMax(Slot.values()[Math.min(ii, 2)]);
                _slots[ii].setBackground(BComponent.DEFAULT,
                        new ImageBackground(ImageBackground.FRAME_X, _slotbgs[0]));
                _slots[ii].setBackground(BComponent.DISABLED,
                        new ImageBackground(ImageBackground.FRAME_X, _slotbgs[0]));
                _slots[ii].setBackground(BComponent.HOVER,
                        new ImageBackground(ImageBackground.FRAME_X, _slothbgs[0]));
            }
            if (_mode.isEnabled()) {
                for (BCheckBox scen : _scens) {
                    scen.setEnabled(true);
                }
            }
            break;
        case TEAM_2V2:
            for (int ii = 0; ii < _slots.length; ii++) {
                _slots[ii].setMax(Slot.values()[Math.min(ii, 1)]);
                _slots[ii].setBackground(BComponent.DEFAULT,
                        new ImageBackground(ImageBackground.FRAME_X, _slotbgs[ii / 2 + 1]));
                _slots[ii].setBackground(BComponent.DISABLED,
                        new ImageBackground(ImageBackground.FRAME_X, _slotbgs[ii / 2 + 1]));
                _slots[ii].setBackground(BComponent.HOVER,
                        new ImageBackground(ImageBackground.FRAME_X, _slothbgs[ii / 2 + 1]));
            }
            for (int ii = 0; ii < _scenIds.length; ii++) {
                if (ScenarioInfo.getScenarioInfo(_scenIds[ii]).getTeams() ==
                        ScenarioInfo.Teams.COOP) {
                    _scens[ii].setEnabled(false);
                }
            }
            break;
        }

        _updatingDisplay = false;
    }

    protected void updateDisplay ()
    {
        _updatingDisplay = true;
        if (_tobj.game != null) {
            _rounds.selectItem(Integer.valueOf(_tobj.game.rounds));
            _units.selectItem(Integer.valueOf(_tobj.game.teamSize));
            _duration.selectValue(_tobj.game.duration);
            _speed.selectValue(_tobj.game.speed);
            _mode.selectValue(_tobj.game.mode);
            setSlotTypes(_tobj.game.slots);
            HashSet<String> set = new HashSet<String>();
            CollectionUtil.addAll(set, _tobj.game.scenarios);
            for (int ii = 0; ii < _scenIds.length; ii++) {
                _scens[ii].setSelected(set.contains(_scenIds[ii]));
            }
        }

        boolean canCreate = _tview.canCreate();
        _rounds.setEnabled(canCreate);
        _units.setEnabled(canCreate);
        _duration.setEnabled(canCreate);
        _speed.setEnabled(canCreate);
        for (int ii = 0; ii < _scens.length; ii++) {
            _scens[ii].setEnabled(canCreate);
        }
        for (int ii = 0; ii < _slots.length; ii++) {
            _slots[ii].setEnabled(canCreate);
        }
        _mode.setEnabled(canCreate);
        _create.setEnabled(canCreate);
        if (canCreate) {
            _buttons.removeAll();
            _buttons.add(_create);
        } else {
            _buttons.removeAll();
            _buttons.add(_ownerIcon);
        }
        _updatingDisplay = false;
        modeUpdated();
    }

    protected ParlorGameConfig makeConfig ()
    {
        ParlorGameConfig config = new ParlorGameConfig();
        config.rounds = (Integer)_rounds.getSelectedItem();
        config.teamSize = (Integer)_units.getSelectedItem();
        config.duration = (BangConfig.Duration)_duration.getSelectedValue();
        config.speed = (BangConfig.Speed)_speed.getSelectedValue();
        config.mode = (ParlorGameConfig.Mode)_mode.getSelectedValue();
        storeSlotConfiguration(config.slots);

        // sotre these to preferences because we don't track them magically
        for (int ii = 0; ii < _slots.length; ii++) {
            BangPrefs.config.setValue("parlor.slot." + ii, _slots[ii].getType().ordinal());
        }

        ArrayList<String> scens = new ArrayList<String>();
        for (int ii = 0; ii < _scenIds.length; ii++) {
            if (_scens[ii].isSelected() && _scens[ii].isEnabled()) {
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
            int pcount = config.getCount(Slot.HUMAN) + config.getCount(Slot.TINCAN);
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
        _tobj.service.startMatchMaking(config, bdata, rl);
    }

    protected Object[] makeBoxItems (int low, int high)
    {
        ArrayList<Integer> items = new ArrayList<Integer>();
        for (int ii = low; ii <= high; ii++) {
            items.add(ii);
        }
        return items.toArray(new Integer[items.size()]);
    }

    protected void setSlotTypes (Slot[] slots)
    {
        for (int ii = 0; ii < slots.length; ii++) {
            _slots[ii].setType(slots[ii]);
        }
    }

    protected boolean slotsChanged (Slot[] slots)
    {
        for (int ii = 0; ii < slots.length; ii++) {
            if (_slots[ii].getType() != slots[ii]) {
                return true;
            }
        }
        return false;
    }

    protected void storeSlotConfiguration (Slot[] slots)
    {
        for (int ii = 0; ii < slots.length; ii++) {
            slots[ii] = _slots[ii].getType();
        }
    }

    protected class PlayerSlotButton extends BLabel
    {
        public PlayerSlotButton (Slot max) {
            super((String)null);
            setStyleClass("n_state_button");
            _icons = new SubimageIcon[_psicons.length];
            System.arraycopy(_psicons, 0, _icons, 0, _icons.length);
            setMax(max);
        }

        public void setMax (Slot max) {
            _max = max.ordinal();
            if (max == Slot.HUMAN) {
                setEnabled(false);
            }
            if (_type == null || _type.ordinal() > _max) {
                setType(max);
            }
        }

        public Slot getType () {
            return _type;
        }

        public void setType (Slot type) {
            if (type.ordinal() > _max) {
                return;
            }
            if (_type != type) {
                _type = type;
                setIcon(_icons[_type.ordinal()]);
                emitEvent(new ActionEvent(this, 0L, 0, "typeSet"));
            }
            if (isAdded()) {
                getWindow().getRootNode().tipTextChanged(this);
            }
        }

        @Override
        public boolean dispatchEvent (BEvent event) {
            if (isEnabled() && event instanceof MouseEvent) {
                MouseEvent mev = (MouseEvent)event;
                if (mev.getType() == MouseEvent.MOUSE_PRESSED) {
                    // toggle to the next type
                    setType(Slot.values()[(_type.ordinal() + 1) % (_max + 1)]);
                    return true;
                } else {
                    return super.dispatchEvent(event);
                }
            }
            return super.dispatchEvent(event);
        }

        @Override
        public String getTooltipText () {
            return _msgs.get("m.slot_tip_" + _type);
        }

        protected SubimageIcon[] _icons;
        protected int _max;
        protected Slot _type;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected TableGameObject _tobj;
    protected StatusLabel _status;
    protected TableGameView _tview;

    protected BComboBox _rounds, _units, _duration, _speed, _mode;

    protected SubimageIcon[] _psicons;
    protected BImage[] _slotbgs, _slothbgs;
    protected PlayerSlotButton[] _slots;

    protected String[] _scenIds;
    protected BCheckBox[] _scens;
    protected BButton _create;
    protected BContainer _buttons;
    protected BLabel _ownerIcon;

    protected boolean _updatingDisplay = false;

    // only usable by admins for now
    protected BTextField _board;

    protected static final int HUMAN = 0;
    protected static final int TINCAN = 1;
    protected static final int NONE = 2;
}
