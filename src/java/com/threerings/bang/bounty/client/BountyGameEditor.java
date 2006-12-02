//
// $Id$

package com.threerings.bang.bounty.client;

import java.util.ArrayList;

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

import com.threerings.bang.client.util.StateSaver;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.scenario.ScenarioInfo;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BangUtil;

import com.threerings.bang.bounty.data.BoardInfo;
import com.threerings.bang.bounty.data.OfficeCodes;
import com.threerings.bang.bounty.data.OfficeObject;

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
        add(cpanel);

        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        buttons.add(new BButton(_msgs.get("m.run_game"), this, "run_game"));
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
            new StateSaver("bounty.punits" + ii, _opponents);
        }
        for (int oo = 0; oo < _oppunits.length; oo++) {
            for (int ii = 0; ii < _oppunits[oo].length; ii++) {
                _oppunits[oo][ii].setItems(ii == 0 ? _bsunits : _units);
                _oppunits[oo][ii].selectItem(0);
                new StateSaver("bounty.oppunits" + oo + "." + ii, _opponents);
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
            // TODO

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
        Integer pcount = (Integer)_opponents.getSelectedItem();
        BComboBox.Item scitem = (BComboBox.Item)_scenario.getSelectedItem();
        if (pcount == null || scitem == null) {
            return;
        }
        int players = pcount + 1;

        // if we had a board selected, try to preserve it
        BoardInfo oinfo = (BoardInfo)_board.getSelectedItem();
        _board.clearItems();
        for (BoardInfo info : _offobj.boards) {
            if (info.matches(players, (String)scitem.value)) {
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
        for (int oo = 0; oo < _oppunits.length; oo++) {
            for (int ii = 0; ii < _oppunits[oo].length; ii++) {
                _oppunits[oo][ii].setEnabled(pcount > oo);
            }
        }
    }

    protected ActionListener _refigger = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            refigure();
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected OfficeObject _offobj;

    protected BComboBox _opponents;
    protected BComboBox _scenario;
    protected BComboBox _board;
    protected BComboBox[] _punits;
    protected BComboBox[][] _oppunits;

    protected ArrayList<BComboBox.Item> _bsunits, _units;

    protected static final Integer[] OPPONENTS = new Integer[] { 1, 2, 3 };

    // default to max team size plus one for big shot
    protected static final int MAX_BOUNTY_UNITS = GameCodes.MAX_TEAM_SIZE + 1;
}
