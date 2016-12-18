//
// $Id$

package com.threerings.bang.bounty.client;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import com.jme.util.export.binary.BinaryExporter;
import com.jme.util.export.binary.BinaryImporter;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
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

import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.client.util.ReportingListener;
import com.threerings.bang.client.util.StateSaver;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.SaloonCodes;

import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.Criterion;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.card.Card;
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
        setModal(true);

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(OfficeCodes.OFFICE_MSGS);
        _offobj = offobj;

        // create our configuration interface
        BContainer cpanel = new BContainer(new TableLayout(8, 5, 5)), row;
        addRow(cpanel, "m.opponents").add(_opponents = new BComboBox(OPPONENTS));
        cpanel.add(new Spacer(10, 10));
        addRow(cpanel, "m.scenario").add(_scenario = new BComboBox());
        cpanel.add(new Spacer(10, 10));
        addRow(cpanel, "m.board").add(_board = new BComboBox());

        addRow(cpanel, "m.duration").add(_duration = new BComboBox());
        cpanel.add(new Spacer(10, 10));
        addRow(cpanel, "m.speed").add(_speed = new BComboBox());
        cpanel.add(new Spacer(10, 10));
        addRow(cpanel, "m.respawn").add(_respawn = new BCheckBox(""));

        addRow(cpanel, "m.other").add(_sdata = new BTextField());
        _sdata.setPreferredSize(80, -1);
        add(cpanel);

        BContainer bpanel = new BContainer(new TableLayout(5, 5, 5));
        addRow(bpanel, "m.min_weight").add(_minweight = new BComboBox(CUTOFFS));
        bpanel.add(new Spacer(10, 10));
        BContainer cardp = addRow(bpanel, "m.cards");
        for (int ii = 0; ii < _cardsel.length; ii++) {
            cardp.add(_cardsel[ii] = new BComboBox());
        }
        add(bpanel);

        _ppanel = new BContainer(new TableLayout(7, 5, 15));
        _ppanel.add(new BLabel(""));
        _ppanel.add(new BLabel(_msgs.get("m.bplayer_start_spot"), "table_header"));
        _ppanel.add(new BLabel(_msgs.get("m.bplayer_team"), "table_header"));
        _ppanel.add(new BLabel(_msgs.get("m.bplayer_skill"), "table_header"));
        _ppanel.add(new BLabel(_msgs.get("m.bplayer_bigshot"), "table_header"));
        _ppanel.add(new BLabel(_msgs.get("m.bplayer_units"), "table_header"));
        _ppanel.add(new BLabel(""));

        for (int pp = 0; pp < _players.length; pp++) {
            _players[pp] = new BangConfig.Player();
            String lmsg = (pp == 0) ? "m.player_units" : "m.opp_units";
            _ppanel.add(new BLabel(_msgs.get(lmsg)));
            _ppanel.add(_starts[pp] = new BLabel("", "bge_table_rdata"));
            _ppanel.add(_teams[pp] = new BLabel("", "bge_table_rdata"));
            _ppanel.add(_skills[pp] = new BLabel("", "bge_table_rdata"));
            _ppanel.add(_bigshots[pp] = new BLabel(""));
            _ppanel.add(_units[pp] = new BLabel("", "bge_table_tdata"));
            _units[pp].setPreferredSize(300, -1);
            playerUpdated(pp);

            final int pidx = pp;
            BButton edit = new BButton(_msgs.get("m.edit_player"), new ActionListener() {
                public void actionPerformed (ActionEvent event) {
                    BountyPlayerEditor editor = new BountyPlayerEditor(
                        _ctx, BountyGameEditor.this, (Integer)_opponents.getSelectedItem(),
                        pidx, _players[pidx]);
                    _ctx.getBangClient().displayPopup(editor, true);
                }
            }, "edit_player");
            edit.setStyleClass("alt_button");
            _ppanel.add(edit);
        }
        add(_ppanel);

        BContainer ctpanel = new BContainer(new TableLayout(2, 5, 5));
        ArrayList<BComboBox.Item> types = new ArrayList<BComboBox.Item>();
        for (CriterionEditor.Type type : CriterionEditor.Type.values()) {
            String msg = "m.type_" + StringUtil.toUSLowerCase(type.toString());
            types.add(new BComboBox.Item(type, _msgs.get(msg)));
        }
        row = addRow(ctpanel, "m.add_criterion");
        row.add(_ctype = new BComboBox(types));
        row.add(new BButton(_msgs.get("m.add"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                CriterionEditor.Type type = (CriterionEditor.Type)_ctype.getSelectedValue();
                _criteria.add(CriterionEditor.createEditor(_ctx, type));
                BountyGameEditor.this.pack();
                BountyGameEditor.this.center();
            }
        }, "add_crit"));

        // add a panel that will contain our criterion
        ctpanel.add(new BLabel(""));
        ctpanel.add(_criteria = new BContainer(GroupLayout.makeVStretch()));
        ((GroupLayout)_criteria.getLayoutManager()).setPolicy(GroupLayout.NONE);
        add(ctpanel);

        // add a status label
        add(_status = new StatusLabel(_ctx));

        // add some control buttons
        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        buttons.add(new BButton(_msgs.get("m.run_game"), this, "run_game"));
        buttons.add(new BButton(_msgs.get("m.load_game"), this, "load_game"));
        buttons.add(new BButton(_msgs.get("m.save_game"), this, "save_game"));
        buttons.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        add(buttons);

        // configure our various drop downs
        _ctype.selectItem(0);
        new StateSaver("bounty.crit_type", _ctype);
        _opponents.selectItem(1);
        new StateSaver("bounty.opponents", _opponents);

        String townId = _ctx.getUserObject().townId;
        ArrayList<BComboBox.Item> scens = new ArrayList<BComboBox.Item>();
        for (ScenarioInfo info : ScenarioInfo.getScenarios(townId, true)) {
            scens.add(new BComboBox.Item(
                          info.getIdent(), _ctx.xlate(GameCodes.GAME_MSGS, info.getName())));
        }
        _scenario.setItems(scens);
        _scenario.selectItem(0);
        new StateSaver("bounty.scenario", _scenario);

        _duration.setItems(new BComboBox.Item[] {
            new BComboBox.Item(BangConfig.Duration.QUICK,
                               _ctx.xlate(SaloonCodes.SALOON_MSGS, "m.dur_quick")),
            new BComboBox.Item(BangConfig.Duration.NORMAL,
                               _ctx.xlate(SaloonCodes.SALOON_MSGS, "m.dur_normal")),
            new BComboBox.Item(BangConfig.Duration.LONG,
                               _ctx.xlate(SaloonCodes.SALOON_MSGS, "m.dur_long"))
        });
        _duration.selectItem(1);
        new StateSaver("bounty.duration", _duration);

        _speed.setItems(new BComboBox.Item[] {
            new BComboBox.Item(BangConfig.Speed.FAST,
                               _ctx.xlate(SaloonCodes.SALOON_MSGS, "m.sp_fast")),
            new BComboBox.Item(BangConfig.Speed.NORMAL,
                               _ctx.xlate(SaloonCodes.SALOON_MSGS, "m.sp_normal")),
            new BComboBox.Item(BangConfig.Speed.SLOW,
                               _ctx.xlate(SaloonCodes.SALOON_MSGS, "m.sp_slow"))
        });
        _speed.selectItem(1);
        new StateSaver("bounty.speed", _speed);

        _respawn.setSelected(true);
        new StateSaver("bounty.respawn", _respawn);
        _minweight.selectItem(0);
        new StateSaver("bounty.min_weight", _minweight);

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
                _offobj.service.testBountyGame(createConfig(), rl);
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
        box.add(new BLabel(_msgs.get(label), "table_label"));
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
        int players = Math.max(oppcount + 1, 2);

        // if we had a board selected, try to preserve it
        BoardInfo oinfo = (BoardInfo)_board.getSelectedItem();
        _board.clearItems();
        for (BoardInfo info : _offobj.boards) {
            if (info.matches(players, scenario)) {
                _board.addItem(info);
            }
        }
        _board.selectItem(oinfo);
        if (_board.getSelectedIndex() == -1 && _board.getItemCount() > 0) {
            _board.selectItem(0);
        }

        // only show as many players as we have
        showPlayerGrid(oppcount);

        // compute the set of available cards
        _cards.clear();
        _cards.add(new BComboBox.Item(null, _msgs.get("m.none")));
        ScenarioInfo info = ScenarioInfo.getScenarioInfo(scenario);
        for (Card card : Card.getCards()) {
            if (card.isPlayable(info, _ctx.getUserObject().townId)) {
                _cards.add(new BComboBox.Item(card.getType(), _msgs.xlate(card.getName())));
            }
        }
        Collections.sort(_cards);
        for (BComboBox cardsel : _cardsel) {
            cardsel.setItems(_cards);
            if (cardsel.getSelectedItem() == null) {
                cardsel.selectItem(0);
            }
        }

        if (isAdded()) {
            pack();
            center();
        }
    }

    protected void playerUpdated (int pidx)
    {
        _starts[pidx].setText(String.valueOf(_players[pidx].startSpot));
        _teams[pidx].setText(_players[pidx].teamIdx == -1 ? _msgs.get("m.no_teams") :
                             _msgs.get("m.on_team", String.valueOf(_players[pidx].teamIdx+1)));
        _skills[pidx].setText(pidx == 0 ? "" : String.valueOf(_players[pidx].skill));
        String bsname = _players[pidx].bigShot == null ?  _msgs.get("m.none") :
            _msgs.xlate(UnitConfig.getName(_players[pidx].bigShot));
        _bigshots[pidx].setText(bsname);
        StringBuilder buf = new StringBuilder();
        if (_players[pidx].units != null) {
            for (String unit : _players[pidx].units) {
                if (buf.length() > 0) {
                    buf.append(", ");
                }
                buf.append(_msgs.xlate(UnitConfig.getName(unit)));
            }
        }
        _units[pidx].setText(buf.toString());
    }

    protected BangConfig createConfig ()
    {
        BangConfig config = new BangConfig();
        config.duration = (BangConfig.Duration)_duration.getSelectedValue();
        config.speed = (BangConfig.Speed)_speed.getSelectedValue();
        config.addRound((String)_scenario.getSelectedValue(),
                        ((BoardInfo)_board.getSelectedItem()).name, null, _sdata.getText());
        config.respawnUnits = _respawn.isSelected();
        config.minWeight = (Integer)_minweight.getSelectedItem();

        String[] cards = new String[_cardsel.length];
        for (int ii = 0; ii < _cardsel.length; ii++) {
            cards[ii] = (String)_cardsel[ii].getSelectedValue();
        }
        for (int ii = 0, ll = 1+(Integer)_opponents.getSelectedItem(); ii < ll; ii++) {
            if (ii == 0) {
                _players[ii].cards = cards;
            }
            config.plist.add(_players[ii]);
        }

        for (int ii = 0; ii < _criteria.getComponentCount(); ii++) {
            config.criteria.add(((CriterionEditor)_criteria.getComponent(ii)).getCriterion());
        }

        return config;
    }

    protected void displayConfig (BangConfig config)
    {
        _scenario.selectValue(config.rounds.get(0).scenario);
        _opponents.selectItem(Integer.valueOf(config.plist.size()-1));
        _duration.selectValue(config.duration);
        _speed.selectValue(config.speed);
        _respawn.setSelected(config.respawnUnits);
        _minweight.selectItem(Integer.valueOf(config.minWeight));
        _sdata.setText(config.rounds.get(0).sdata);

        // configure our card selections
        if (config.plist.size() > 0 && config.plist.get(0).cards != null) {
            String[] cards = config.plist.get(0).cards;
            for (int ii = 0; ii < cards.length; ii++) {
                _cardsel[ii].selectValue(cards[ii]);
            }
        }

        // locate and select the correct board
        for (int ii = 0; ii < _board.getItemCount(); ii++) {
            BoardInfo info = (BoardInfo)_board.getItem(ii);
            if (info.name.equals(config.rounds.get(0).board)) {
                _board.selectItem(ii);
                break;
            }
        }

        // configure the players
        for (int pidx = 0; pidx < config.plist.size(); pidx++) {
            _players[pidx] = config.plist.get(pidx);
            playerUpdated(pidx);
        }
        showPlayerGrid(config.plist.size()-1);

        _criteria.removeAll();
        for (Criterion crit : config.criteria) {
            _criteria.add(CriterionEditor.createEditor(_ctx, crit));
        }

        BountyGameEditor.this.pack();
        BountyGameEditor.this.center();
    }

    protected void showPlayerGrid (int oppcount)
    {
        int ccount = _ppanel.getComponentCount() / (_units.length+1);
        for (int pp = 0; pp < _units.length; pp++) {
            for (int cc = 0; cc < ccount; cc++) {
                _ppanel.getComponent(ccount + pp * ccount + cc).setVisible(pp <= oppcount);
            }
        }
    }

    protected void loadGameConfig (String filename)
    {
        try {
            File file = getFile(filename);
            displayConfig((BangConfig)BinaryImporter.getInstance().load(file));
            _status.setStatus(OfficeCodes.OFFICE_MSGS,
                              MessageBundle.tcompose("m.loaded_game", file), false);

        } catch (Exception e) {
            log.warning("Failed to load bounty game.", e);
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
            log.warning("Failed to save bounty game.", e);
            _status.setStatus(OfficeCodes.OFFICE_MSGS,
                              MessageBundle.tcompose("m.save_game_failed", e.getMessage()), true);
        }
    }

    protected File getFile (String filename)
    {
        if (!filename.endsWith(".game")) {
            filename = filename + ".game";
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

    protected BComboBox _opponents, _scenario, _board;
    protected BComboBox _duration, _speed, _minweight;
    protected BComboBox[] _cardsel = new BComboBox[GameCodes.MAX_CARDS];
    protected BCheckBox _respawn;
    protected BTextField _sdata;

    protected BangConfig.Player[] _players = new BangConfig.Player[OPPONENTS.length];
    protected BContainer _ppanel;
    protected BLabel[] _starts = new BLabel[_players.length];
    protected BLabel[] _teams = new BLabel[_players.length];
    protected BLabel[] _skills = new BLabel[_players.length];
    protected BLabel[] _bigshots = new BLabel[_players.length];
    protected BLabel[] _units = new BLabel[_players.length];

    protected BComboBox _ctype;
    protected BContainer _criteria;

    protected ArrayList<BComboBox.Item> _cards = new ArrayList<BComboBox.Item>();

    protected static final Integer[] OPPONENTS = new Integer[] { 0, 1, 2, 3 };

    protected static final Integer[] CUTOFFS = new Integer[] {
        0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
}
