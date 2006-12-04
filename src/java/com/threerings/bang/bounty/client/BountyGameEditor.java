//
// $Id$

package com.threerings.bang.bounty.client;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;

import com.jme.util.export.binary.BinaryExporter;
import com.jme.util.export.binary.BinaryImporter;

import com.jmex.bui.BButton;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.client.util.ReportingListener;
import com.threerings.bang.client.util.StateSaver;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BangUtil;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.Criterion;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.scenario.ScenarioInfo;

import com.threerings.bang.bounty.data.BoardInfo;
import com.threerings.bang.bounty.data.OfficeCodes;
import com.threerings.bang.bounty.data.OfficeObject;

import static com.threerings.bang.Log.log;

/**
 * Allows bounty games to be configured and tested.
 */
public class BountyGameEditor extends BDecoratedWindow
    implements ActionListener
{
    public BountyGameEditor (BangContext ctx, OfficeObject offobj)
    {
        super(ctx.getStyleSheet(), ctx.xlate(OfficeCodes.OFFICE_MSGS, "m.create_bounty_game"));
        setLayoutManager(GroupLayout.makeVert(GroupLayout.CENTER).setGap(20));

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(OfficeCodes.OFFICE_MSGS);
        _offobj = offobj;

        // create our configuration interface
        BContainer cpanel = new BContainer(new TableLayout(2, 5, 5));
        addRow(cpanel, "m.opponents").add(_opponents = new BComboBox(OPPONENTS));
        addRow(cpanel, "m.scenario").add(_scenario = new BComboBox());
        addRow(cpanel, "m.board").add(_board = new BComboBox());
        BContainer row = addRow(cpanel, "m.player_units");
        _punits = new BComboBox[MAX_BOUNTY_UNITS];
        for (int ii = 0; ii < _punits.length; ii++) {
            row.add(_punits[ii] = new BComboBox());
        }
        _oppunits = new BComboBox[OPPONENTS.length][MAX_BOUNTY_UNITS];
        for (int oo = 0; oo < _oppunits.length; oo++) {
            row = addRow(cpanel, "m.opp_units");
            for (int ii = 0; ii < _oppunits[oo].length; ii++) {
                row.add(_oppunits[oo][ii] = new BComboBox());
            }
        }

        ArrayList<BComboBox.Item> types = new ArrayList<BComboBox.Item>();
        for (CriterionEditor.Type type : CriterionEditor.Type.values()) {
            String msg = "m.type_" + type.toString().toLowerCase();
            types.add(new BComboBox.Item(type, _msgs.get(msg)));
        }
        row = addRow(cpanel, "m.add_criterion");
        row.add(_ctype = new BComboBox(types));
        row.add(new BButton(_msgs.get("m.add"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                CriterionEditor.Type type = (CriterionEditor.Type)_ctype.getSelectedValue();
                _criterion.add(CriterionEditor.createEditor(_ctx, type));
                BountyGameEditor.this.pack();
                BountyGameEditor.this.center();
            }
        }, "add_crit"));

        // add a panel that will contain our criterion
        cpanel.add(new BLabel(""));
        cpanel.add(_criterion = new BContainer(GroupLayout.makeVStretch()));
        ((GroupLayout)_criterion.getLayoutManager()).setPolicy(GroupLayout.NONE);
        add(cpanel);

        // add a status label
        add(_status = new StatusLabel(_ctx));

        // add some control buttons
        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        buttons.add(new BButton(_msgs.get("m.run_game"), this, "run_game"));
        buttons.add(new BButton(_msgs.get("m.load_game"), this, "load_game"));
        buttons.add(new BButton(_msgs.get("m.save_game"), this, "save_game"));
        buttons.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        add(buttons);

        // enumerate our available units
        String townId = _ctx.getUserObject().townId;
        _bsunits = new ArrayList<BComboBox.Item>();
        _units = new ArrayList<BComboBox.Item>();
        for (int tt = 0, ll = BangUtil.getTownIndex(townId); tt <= ll; tt++) {
            String tid = BangCodes.TOWN_IDS[tt];
            _bsunits.add(new BComboBox.Item(null, _msgs.get("m.unit_none")));
            for (UnitConfig uconf : UnitConfig.getTownUnits(tid, UnitConfig.Rank.BIGSHOT)) {
                _bsunits.add(new BComboBox.Item(uconf.type, _msgs.xlate(uconf.getName())));
            }
            _units.add(new BComboBox.Item(null, _msgs.get("m.unit_none")));
            for (UnitConfig uconf : UnitConfig.getTownUnits(tid, UnitConfig.Rank.NORMAL)) {
                _units.add(new BComboBox.Item(uconf.type, _msgs.xlate(uconf.getName())));
            }
        }

        // configure our various drop downs
        _ctype.selectItem(0);
        new StateSaver("bounty.crit_type", _ctype);
        _opponents.selectItem(0);
        new StateSaver("bounty.opponents", _opponents);

        ArrayList<BComboBox.Item> scens = new ArrayList<BComboBox.Item>();
        for (ScenarioInfo info : ScenarioInfo.getScenarios(townId, true)) {
            scens.add(new BComboBox.Item(
                          info.getIdent(), _ctx.xlate(GameCodes.GAME_MSGS, info.getName())));
        }
        _scenario.setItems(scens);
        _scenario.selectItem(0);
        new StateSaver("bounty.scenario", _scenario);

        for (int ii = 0; ii < _punits.length; ii++) {
            _punits[ii].setItems(ii == 0 ? _bsunits : _units);
            _punits[ii].selectItem(0);
            new StateSaver("bounty.punits" + ii, _punits[ii]);
        }
        for (int oo = 0; oo < _oppunits.length; oo++) {
            for (int ii = 0; ii < _oppunits[oo].length; ii++) {
                _oppunits[oo][ii].setItems(ii == 0 ? _bsunits : _units);
                _oppunits[oo][ii].selectItem(0);
                new StateSaver("bounty.oppunits" + oo + "." + ii, _oppunits[oo][ii]);
            }
        }

        // finally add our board figurer listeners and refigure
        _opponents.addListener(_refigger);
        _scenario.addListener(_refigger);
        refigure();
    }

    // from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if ("run_game".equals(event.getAction())) {
            ReportingListener rl = new ReportingListener(
                _ctx, OfficeCodes.OFFICE_MSGS, "m.test_game_failed");
            try {
                _offobj.service.testBountyGame(_ctx.getClient(), createConfig(), rl);
                _ctx.getBangClient().clearPopup(this, true);
            } catch (Exception e) {
                String msg = MessageBundle.tcompose("m.test_game_failed", e.getMessage());
                _status.setStatus(OfficeCodes.OFFICE_MSGS, msg, true);
            }

        } else if ("save_game".equals(event.getAction())) {
            OptionDialog.ResponseReceiver receiver = new OptionDialog.ResponseReceiver() {
                public void resultPosted (int button, Object result) {
                    if (button != OptionDialog.OK_BUTTON) {
                        return;
                    }
                    saveGameConfig((String)result);
                }
            };
            OptionDialog.showStringDialog(
                _ctx, OfficeCodes.OFFICE_MSGS, "m.save_name",
                new String[] { "m.save_game", "m.cancel" }, 400, "", receiver);

        } else if ("load_game".equals(event.getAction())) {
            OptionDialog.ResponseReceiver receiver = new OptionDialog.ResponseReceiver() {
                public void resultPosted (int button, Object result) {
                    if (button != OptionDialog.OK_BUTTON) {
                        return;
                    }
                    loadGameConfig((String)result);
                }
            };
            OptionDialog.showStringDialog(
                _ctx, OfficeCodes.OFFICE_MSGS, "m.load_name",
                new String[] { "m.load_game", "m.cancel" }, 400, "", receiver);

        } else if ("dismiss".equals(event.getAction())) {
            _ctx.getBangClient().clearPopup(this, true);
        }
    }

    protected BContainer addRow (BContainer box, String label)
    {
        box.add(new BLabel(_msgs.get(label)));
        BContainer row = GroupLayout.makeHBox(GroupLayout.LEFT);
        box.add(row);
        return row;
    }

    /**
     * Configures the boards dropdown with those that support the specified player count and
     * scenario selection.
     */
    protected void refigure ()
    {
        Integer oppcount = (Integer)_opponents.getSelectedItem();
        String scenario = (String)_scenario.getSelectedValue();
        if (oppcount == null || scenario == null) {
            return;
        }
        int players = oppcount + 1;

        // if we had a board selected, try to preserve it
        BoardInfo oinfo = (BoardInfo)_board.getSelectedItem();
        _board.clearItems();
        for (BoardInfo info : _offobj.boards) {
            if (info.matches(players, scenario)) {
                _board.addItem(info);
                if (oinfo != null && info.name.equals(oinfo.name)) {
                    _board.selectItem(_board.getItemCount()-1);
                }
            }
        }
        if (_board.getSelectedIndex() == -1 && _board.getItemCount() > 0) {
            _board.selectItem(0);
        }

        // enable or disable the opponent unit selection grid
        enableUnitGrid(oppcount);
    }

    protected BangConfig createConfig ()
    {
        BangConfig config = new BangConfig();
        config.addRound((String)_scenario.getSelectedValue(),
                        ((BoardInfo)_board.getSelectedItem()).name, null);

        config.addPlayer((String)_punits[0].getSelectedValue(), getTeam(_punits));
        for (int ii = 0, ll = (Integer)_opponents.getSelectedItem(); ii < ll; ii++) {
            config.addPlayer((String)_oppunits[ii][0].getSelectedValue(), getTeam(_oppunits[ii]));
        }

        for (int ii = 0; ii < _criterion.getComponentCount(); ii++) {
            config.criterion.add(((CriterionEditor)_criterion.getComponent(ii)).getCriterion());
        }

        return config;
    }

    protected void displayConfig (BangConfig config)
    {
        _scenario.selectValue(config.rounds.get(0).scenario);
        _opponents.selectItem(Integer.valueOf(config.teams.size()-1));

        // locate and select the correct board
        for (int ii = 0; ii < _board.getItemCount(); ii++) {
            BoardInfo info = (BoardInfo)_board.getItem(ii);
            if (info.name.equals(config.rounds.get(0).board)) {
                _board.selectItem(ii);
                break;
            }
        }

        // configure the units
        for (int pidx = 0; pidx < config.teams.size(); pidx++) {
            BangConfig.Player player = config.teams.get(pidx);
            BComboBox[] units = (pidx == 0) ? _punits : _oppunits[pidx-1];
            units[0].selectValue(player.bigShot);
            for (int uu = 1; uu < units.length; uu++) {
                units[uu].selectValue((player.team != null && player.team.length > (uu-1)) ?
                                      player.team[uu-1] : null);
            }
        }
        enableUnitGrid(config.teams.size()-1);

        _criterion.removeAll();
        for (Criterion crit : config.criterion) {
            _criterion.add(CriterionEditor.createEditor(_ctx, crit));
        }

        BountyGameEditor.this.pack();
        BountyGameEditor.this.center();
    }

    protected void enableUnitGrid (int oppcount)
    {
        for (int oo = 0; oo < _oppunits.length; oo++) {
            for (int ii = 0; ii < _oppunits[oo].length; ii++) {
                _oppunits[oo][ii].setEnabled(oppcount > oo);
            }
        }
    }

    protected String[] getTeam (BComboBox[] units)
    {
        ArrayList<String> team = new ArrayList<String>();
        for (int ii = 1; ii < units.length; ii++) {
            String unit = (String)units[ii].getSelectedValue();
            if (unit != null) {
                team.add(unit);
            }
        }
        return team.toArray(new String[team.size()]);
    }

    protected void loadGameConfig (String filename)
    {
        try {
            File file = getFile(filename);
            displayConfig((BangConfig)BinaryImporter.getInstance().load(file));
            _status.setStatus(OfficeCodes.OFFICE_MSGS,
                              MessageBundle.tcompose("m.loaded_game", file), false);

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to load bounty game.", e);
            _status.setStatus(OfficeCodes.OFFICE_MSGS,
                              MessageBundle.tcompose("m.load_game_failed", e.getMessage()), true);
        }
    }

    protected void saveGameConfig (String filename)
    {
        try {
            File file = getFile(filename);
            BinaryExporter.getInstance().save(createConfig(), file);
            _status.setStatus(OfficeCodes.OFFICE_MSGS,
                              MessageBundle.tcompose("m.saved_game", file), false);

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to save bounty game.", e);
            _status.setStatus(OfficeCodes.OFFICE_MSGS,
                              MessageBundle.tcompose("m.save_game_failed", e.getMessage()), true);
        }
    }

    protected File getFile (String filename)
    {
        if (!filename.endsWith(".dat")) {
            filename = filename + ".dat";
        }
        return new File(System.getProperty("user.home") + File.separator +
                        "Desktop" + File.separator + filename);
    }

    protected ActionListener _refigger = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            refigure();
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected OfficeObject _offobj;
    protected StatusLabel _status;

    protected BComboBox _opponents;
    protected BComboBox _scenario;
    protected BComboBox _board;
    protected BComboBox[] _punits;
    protected BComboBox[][] _oppunits;

    protected BComboBox _ctype;
    protected BContainer _criterion;

    protected ArrayList<BComboBox.Item> _bsunits, _units;

    protected static final Integer[] OPPONENTS = new Integer[] { 1, 2, 3 };

    // default to max team size plus one for big shot
    protected static final int MAX_BOUNTY_UNITS = GameCodes.MAX_TEAM_SIZE + 1;
}
