//
// $Id$

package com.threerings.bang.saloon.client;

import java.util.ArrayList;
import java.util.HashSet;

import com.jmex.bui.BButton;
import com.jmex.bui.BCheckBox;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;

import com.samskivert.util.CollectionUtil;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.util.ScenarioUtil;

import com.threerings.bang.saloon.data.ParlorGameConfig;
import com.threerings.bang.saloon.data.ParlorObject;
import com.threerings.bang.saloon.data.SaloonCodes;

/**
 * Handles the interface for configuring a game in a Back Parlor.
 */
public class ParlorGameConfigView extends BContainer
    implements AttributeChangeListener, ActionListener
{
    public ParlorGameConfigView (BangContext ctx)
    {
        super(new BorderLayout(0, 10));
        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);

        BContainer main = new BContainer(
            GroupLayout.makeHoriz(GroupLayout.NONE, GroupLayout.CENTER,
                                  GroupLayout.NONE));
        ((GroupLayout)main.getLayoutManager()).setGap(25);
        add(main, BorderLayout.CENTER);

        // create our various combo boxen
        BContainer combos = new BContainer(new TableLayout(2, 5, 5));
        for (int ii = 0; ii < _boxes.length; ii++) {
            combos.add(
                BangUI.createLabel(_msgs, BOX_LABELS[ii], "match_label"));
            combos.add(_boxes[ii] = new BComboBox(makeBoxItems(ii)));
            _boxes[ii].selectItem(BOX_CFGS[3*ii+2]);
            _boxes[ii].addListener(this);
        }
        main.add(combos);

        // create our scenario toggles
        BContainer scenbox = new BContainer(new BorderLayout());
        scenbox.add(BangUI.createLabel(_msgs, "m.scenarios", "match_header"),
                    BorderLayout.NORTH);
        BContainer checkboxen = new BContainer(
            GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP,
                                 GroupLayout.EQUALIZE));
        checkboxen.setStyleClass("match_scenarios");
        _scenIds = ScenarioUtil.getScenarios(_ctx.getUserObject().townId);
        _scens = new BCheckBox[_scenIds.length];
        for (int ii = 0; ii < _scens.length; ii++) {
            _scens[ii] = new BCheckBox(
                _ctx.xlate(GameCodes.GAME_MSGS, "m.scenario_" + _scenIds[ii]));
            _scens[ii].setSelected(true);
            _scens[ii].addListener(this);
            checkboxen.add(_scens[ii]);
        }
        BScrollPane pane = new BScrollPane(checkboxen);
        ((BorderLayout)pane.getLayoutManager()).setGaps(3, 5);
        scenbox.add(pane, BorderLayout.CENTER);
        scenbox.add(new BLabel(" ", "match_header"), BorderLayout.SOUTH);
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
            _parobj.service.startMatchMaking(_ctx.getClient(), makeConfig());
        } else {
            _parobj.service.updateGameConfig(_ctx.getClient(), makeConfig());
        }
    }

    protected void updateDisplay ()
    {
        if (_parobj.game != null) {
            _boxes[0].selectItem(Integer.valueOf(_parobj.game.rounds));
            _boxes[1].selectItem(Integer.valueOf(_parobj.game.players));
            _boxes[2].selectItem(Integer.valueOf(_parobj.game.tinCans));
            _boxes[3].selectItem(Integer.valueOf(_parobj.game.teamSize));

            HashSet<String> set = new HashSet<String>();
            CollectionUtil.addAll(set, _parobj.game.scenarios);
            for (int ii = 0; ii < _scenIds.length; ii++) {
                _scens[ii].setSelected(set.contains(_scenIds[ii]));
            }
        }

        boolean canCreate =
            _ctx.getUserObject().handle.equals(_parobj.info.creator) ||
            !_parobj.onlyCreatorStart;
        for (int ii = 0; ii < _boxes.length; ii++) {
            _boxes[ii].setEnabled(canCreate);
        }
        for (int ii = 0; ii < _scens.length; ii++) {
            _scens[ii].setEnabled(canCreate);
        }
        _create.setEnabled(canCreate);
    }

    protected ParlorGameConfig makeConfig ()
    {
        ParlorGameConfig config = new ParlorGameConfig();
        config.rounds = (Integer)_boxes[0].getSelectedItem();
        config.players = (Integer)_boxes[1].getSelectedItem();
        config.tinCans = (Integer)_boxes[2].getSelectedItem();
        config.teamSize = (Integer)_boxes[3].getSelectedItem();
        ArrayList<String> scens = new ArrayList<String>();
        for (int ii = 0; ii < _scenIds.length; ii++) {
            if (_scens[ii].isSelected()) {
                scens.add(_scenIds[ii]);
            }
        }
        config.scenarios = scens.toArray(new String[scens.size()]);
        return config;
    }

    protected Object[] makeBoxItems (int index)
    {
        ArrayList<Integer> items = new ArrayList<Integer>();
        for (int ii = BOX_CFGS[3*index]; ii <= BOX_CFGS[3*index+1]; ii++) {
            items.add(ii);
        }
        return items.toArray(new Integer[items.size()]);
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected ParlorObject _parobj;

    protected BComboBox[] _boxes = new BComboBox[4];
    protected String[] _scenIds;
    protected BCheckBox[] _scens;
    protected BButton _create;

    protected static final String[] BOX_LABELS = {
        "m.rounds", "m.players", "m.opponents", "m.units" };

    protected static final int[] BOX_CFGS = {
        1, GameCodes.MAX_ROUNDS, 0, // rounds
        1, GameCodes.MAX_PLAYERS, 2, // players
        0, GameCodes.MAX_PLAYERS-1, 0, // tin cans
        1, GameCodes.MAX_TEAM_SIZE, 2, // units per team
    };
}
