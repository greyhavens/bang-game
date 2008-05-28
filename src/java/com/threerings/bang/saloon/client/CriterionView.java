//
// $Id$

package com.threerings.bang.saloon.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.util.StateSaver;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.SaloonCodes;

/**
 * Displays the set of configurable game criterion.
 */
public abstract class CriterionView extends BContainer
{
    public CriterionView (BangContext ctx, String saver)
    {
        super(new BorderLayout(5, 5));

        _saver = saver;
        _ctx = ctx;
        MessageBundle msgs = _ctx.getMessageManager().getBundle(
            SaloonCodes.SALOON_MSGS);

        TableLayout tlay = new TableLayout(2, 3, 25);
        tlay.setHorizontalAlignment(TableLayout.CENTER);
        tlay.setVerticalAlignment(TableLayout.CENTER);
        tlay.setEqualRows(true);
        BContainer table = new BContainer(tlay);
        add(table, BorderLayout.CENTER);

        table.add(BangUI.createLabel(msgs, "m.rounds", "match_label"));
        BContainer row = new BContainer(GroupLayout.makeHStretch());
        for (int ii = 0; ii < _rounds.length; ii++) {
            row.add(_rounds[ii] = new BCheckBox("" + (ii+1)));
            _rounds[ii].setSelected(ii == 0);
            new StateSaver(saver + ".rounds." + ii, _rounds[ii]);
        }
        table.add(row);

        table.add(BangUI.createLabel(msgs, "m.players", "match_label"));
        row = new BContainer(GroupLayout.makeHStretch());
        for (int ii = 0; ii < _players.length; ii++) {
            row.add(_players[ii] = new BCheckBox("" + (ii+2)));
            _players[ii].setSelected(true);
            new StateSaver(saver + ".players." + ii, _players[ii]);
        }
        table.add(row);

        addModeControl(msgs, table);
        if (_mode != null) {
            _mode.selectItem(0);
            new StateSaver(saver + ".mode", _mode);
        }

        table.add(BangUI.createLabel(msgs, "m.range", "match_label"));
        table.add(_range = new BComboBox(xlate(msgs, RANGE)));
        _range.selectItem(0);
        new StateSaver(saver + ".range", _range);

        _prev = new BCheckBox(msgs.get("m.allow_previous"));
        _prev.setSelected(false);
        new StateSaver(saver + ".previous", _prev);

        // only show the _prev checkbox if we're past Frontier Town because it
        // doesn't have any effect in Frontier Town
        if (!_ctx.getUserObject().townId.equals(BangCodes.FRONTIER_TOWN)) {
            table.add(BangUI.createLabel(msgs, "m.prev_scens", "match_label"));
            table.add(_prev);
        }

        row = GroupLayout.makeHBox(GroupLayout.CENTER);
        row.add(_go = new BButton(msgs.get("m.go"), _golist, "match"));
        _go.setStyleClass("big_button");
        add(row, BorderLayout.SOUTH);
    }

    public void reenable ()
    {
        _go.setEnabled(true);
    }

    @Override // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();
        reenable();
    }

    protected void addModeControl (MessageBundle msgs, BContainer table)
    {
        table.add(BangUI.createLabel(msgs, "m.game_mode", "match_label"));
        if (!_ctx.getUserObject().townId.equals(BangCodes.FRONTIER_TOWN)) {
            table.add(_mode = new BComboBox(xlate(msgs, MODE)));
        } else {
            table.add(BangUI.createLabel(msgs, "m." + MODE[1], "match_info"));
        }
    }

    /**
     * Finds or creates a match with the specified criterion.
     */
    protected abstract void findMatch (Criterion criterion);

    protected String[] xlate (MessageBundle msgs, String[] umsgs)
    {
        String[] tmsgs = new String[umsgs.length];
        for (int ii = 0; ii < tmsgs.length; ii++) {
            tmsgs[ii] = msgs.get("m." + umsgs[ii]);
        }
        return tmsgs;
    }

    protected int getModeSelection ()
    {
        if (_mode == null) {
            return Criterion.COMP;
        }
        switch (_mode.getSelectedIndex()) {
        case 0:
            return Criterion.ANY;
        case 2:
            return Criterion.COOP;
        default:
            return Criterion.COMP;
        }
    }

    protected ActionListener _golist = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            // create a criterion instance from our UI configuration
            Criterion criterion = new Criterion();
            criterion.rounds = Criterion.compose(
                _rounds[0].isSelected(), _rounds[1].isSelected(),
                _rounds[2].isSelected());
            criterion.players = Criterion.compose(
                _players[0].isSelected(), _players[1].isSelected(),
                _players[2].isSelected());
            criterion.mode = getModeSelection();
            criterion.range = _range.getSelectedIndex();
            criterion.allowPreviousTowns =
                _prev.isAdded() ? _prev.isSelected() : true;

            // pass the buck onto the controller to do the rest
            _go.setEnabled(false);
            findMatch(criterion);
        }
    };

    protected BangContext _ctx;
    protected String _saver;

    protected BCheckBox[] _rounds = new BCheckBox[GameCodes.MAX_ROUNDS];
    protected BCheckBox[] _players = new BCheckBox[GameCodes.MAX_PLAYERS-1];
    protected BCheckBox[] _aiopps = new BCheckBox[GameCodes.MAX_PLAYERS-1];
    protected BComboBox _mode, _range;
    protected BCheckBox _prev;
    protected BButton _go;

    protected static final String[] MODE = { "any", "comp", "coop" };
    protected static final String[] RANKED = { "both", "ranked", "unranked" };
    protected static final String[] RANGE = { "tight", "loose", "open" };
}
