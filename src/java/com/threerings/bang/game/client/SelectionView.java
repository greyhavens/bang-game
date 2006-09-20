//
// $Id$

package com.threerings.bang.game.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.CollectionUtil;
import com.threerings.util.MessageBundle;

import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.client.bui.SteelWindow;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;

import com.threerings.bang.ranch.client.UnitIcon;
import com.threerings.bang.ranch.client.UnitPalette;
import com.threerings.bang.ranch.client.UnitView;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Displays an interface for selecting a big shot and a starting hand of
 * cards from a player's inventory.
 */
public class SelectionView extends SteelWindow
    implements ActionListener
{
    public SelectionView (BangContext ctx, BangController ctrl,
                          BangConfig config, BangObject bangobj, int pidx)
    {
        super(ctx, "");

        _ctx = ctx;
        _ctrl = ctrl;
        _msgs = _ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);
        _bangobj = bangobj;
        _pidx = pidx;
        _tconfigs = new UnitConfig[bangobj.scenario.getTeamSize(config)];

        BContainer header = GroupLayout.makeHBox(GroupLayout.CENTER);
        String msg = MessageBundle.compose(
            "m.round_header",
            MessageBundle.taint(String.valueOf((bangobj.roundId + 1))),
            bangobj.scenario.getName(), MessageBundle.taint(bangobj.boardName));
        _header.setText(_msgs.xlate(msg));

        // set up our main structural bits
        _contents.setLayoutManager(new BorderLayout(25, 15));

        _side = GroupLayout.makeVBox(GroupLayout.TOP);
        _side.add(_uname = new BLabel("", "pick_unit_name"));
        _side.add(_uview = new UnitView(ctx, true));
        _contents.add(_side, BorderLayout.WEST);

        _center = GroupLayout.makeVBox(GroupLayout.TOP);
        ((GroupLayout)_center.getLayoutManager()).setOffAxisJustification(
            GroupLayout.LEFT);
        _contents.add(_center, BorderLayout.CENTER);

        // we need to create _ready here because code below is going to trigger
        // a call to updateBigShot()
        _buttons.add(_ready = new BButton(
                         _msgs.get("m.ready"), this, "pick_bigshot"));

        // add the mini-cards display to the side
        BContainer cards = GroupLayout.makeHBox(GroupLayout.CENTER);
        for (int ii = 0; ii < _cardsels.length; ii++) {
            cards.add(_cardsels[ii] = new BLabel("", "card_icon"));
        }
        _side.add(cards);

        // create the big shot selection display
        _center.add(new BLabel(_msgs.get("m.select_bigshot"), "pick_subtitle"));
        _units = new UnitPalette(_ctx, _enabler, 4, 1);
        _units.setPaintBorder(true);
        _units.setStyleClass("pick_palette");
        _units.setUser(_ctx.getUserObject(), true);
        _units.selectFirstIcon();
        _center.add(_units);

        // create the card selection display
        _center.add(new BLabel(_msgs.get("m.select_cards"), "pick_subtitle"));
        _center.add(_cards = new CardPalette(_ctx, _ctrl, bangobj, _cardsels));
        _cards.setStyleClass("pick_palette");

        updateBigShot();
    }

    /**
     * Switches to the mode where we pick our teams.
     */
    public void setPickTeamMode (BangConfig config)
    {
        // remove the BigShot UI bits
        while (_side.getComponentCount() > 2) {
            _side.remove(_side.getComponent(2));
        }
        _center.removeAll();
        _waiting = false;

        // add a label for each selectable unit
        int teamSize = _bangobj.scenario.getTeamSize(config);
        _team = new BLabel[teamSize];
        for (int ii = 0; ii < _team.length; ii++) {
            _side.add(_team[ii] = new BLabel("", "pick_team_choice"));
        }

        // create the big shot selection display
        _center.add(new BLabel(_msgs.get("m.pv_assemble"), "pick_subtitle"));
        _units.shutdown();
        _units = new UnitPalette(_ctx, _teamins, 4, 2);
        _units.setPaintBorder(true);
        _units.setStyleClass("pick_palette");
        _units.setSelectable(teamSize);
        _units.selectFirstIcon();
        _center.add(_units);

        // determine which units are available for selection
        ArrayList<UnitConfig> units = new ArrayList<UnitConfig>();
        CollectionUtil.addAll(units, UnitConfig.getTownUnits(_bangobj.townId));
        PlayerObject user = _ctx.getUserObject();
        for (Iterator<UnitConfig> iter = units.iterator(); iter.hasNext(); ) {
            // filter out bigshots and special units
            UnitConfig uc = iter.next();
            if (uc.rank != UnitConfig.Rank.NORMAL || uc.scripCost < 0) {
                iter.remove();
            }
        }
        // sort the units by cost
        Collections.sort(units, new Comparator<UnitConfig>() {
            public int compare (UnitConfig u1, UnitConfig u2) {
                if (u1.scripCost == u2.scripCost) {
                    return u1.type.compareTo(u2.type);
                } else {
                    return u1.scripCost - u2.scripCost;
                }
            }
        });
        _units.setUnits(units.toArray(new UnitConfig[units.size()]), true);

        _ready.setAction("pick_team");
        _ready.setEnabled(false);

        // we need to stay in the exact same location because changing the
        // BGeomView that displays the Big Shot causes the camera to wig out
        Rectangle obounds = getBounds();
        pack();
        setLocation(_x + (obounds.width - _width),
                    _y + (obounds.height - _height));
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent e)
    {
        String cmd = e.getAction();
        if (cmd.equals("pick_bigshot")) {
            UnitIcon icon = _units.getSelectedUnit();
            if (icon == null) {
                return;
            }

            // don't allow double clickage
            _ready.setEnabled(false);
            _waiting = true;

            // determine which cards are selected
            ArrayIntSet cardIds = new ArrayIntSet();
            for (int ii = 0; ii < GameCodes.MAX_CARDS; ii++) {
                CardItem item = _cards.getSelectedCard(ii);
                if (item != null) {
                    cardIds.add(item.getItemId());
                }
            }

            // clear out and disable the palettes
            _units.setSelectable(0);
            _cards.setSelectable(0);

            int bigShotId = icon.getItemId();
            _bangobj.service.selectStarters(
                _ctx.getClient(), bigShotId, cardIds.toIntArray());

        } else if (cmd.equals("pick_team")) {
            // don't allow double clickage
            _ready.setEnabled(false);
            _waiting = true;

            // disable the team selection palette
            _units.setSelectable(0);

            ArrayList<String> units = new ArrayList<String>();
            for (int ii = 0; ii < _tconfigs.length; ii++) {
                if (_tconfigs[ii] != null) {
                    units.add(_tconfigs[ii].type);
                }
            }
            String[] uvec = units.toArray(new String[units.size()]);
            _bangobj.service.selectTeam(_ctx.getClient(), uvec);
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        _units.shutdown();
    }

    protected void updateBigShot ()
    {
        if (!_waiting) {
            _ready.setEnabled(_units.getSelectedUnit() != null);
        }
    }

    protected void updateTeam ()
    {
        if (_waiting) {
            return;
        }

        int uidx = 0, selected = 0, enabled = 0;
        SelectableIcon[] icons = _units.getIcons();
        for (int ii = 0; ii < icons.length; ii++) {
            if (icons[ii].isEnabled()) {
                enabled++;
            }
            if (!icons[ii].isSelected()) {
                continue;
            }
            UnitIcon icon = (UnitIcon)icons[ii];
            _tconfigs[uidx] = icon.getUnit();
            _team[uidx].setText(_ctx.xlate("units", _tconfigs[uidx].getName()));
            uidx++;
            selected++;
        }
        for (int ii = uidx; ii < _team.length; ii++) {
            _tconfigs[ii] = null;
            _team[ii].setText("");
        }

        _ready.setEnabled(selected == _team.length || selected == enabled);
    }

    protected IconPalette.Inspector _enabler = new IconPalette.Inspector() {
        public void iconUpdated (SelectableIcon icon, boolean selected) {
            if (selected) {
                _uname.setText(icon.getText());
                _uview.setUnit(((UnitIcon)icon).getUnit());
            }
            updateBigShot();
        }
    };

    protected UnitPalette.Inspector _teamins = new UnitPalette.Inspector() {
        public void iconUpdated (SelectableIcon icon, boolean selected) {
            updateTeam();
        }
    };

    protected BangContext _ctx;
    protected BangController _ctrl;
    protected MessageBundle _msgs;
    protected BangObject _bangobj;
    protected int _pidx;

    protected BContainer _side, _center;
    protected BLabel _uname;
    protected UnitView _uview;
    protected UnitPalette _units;

    protected CardPalette _cards;
    protected BLabel[] _cardsels = new BLabel[GameCodes.MAX_CARDS];

    protected BLabel[] _team;
    protected UnitConfig[] _tconfigs;

    protected boolean _waiting;
    protected BButton _ready;
}
