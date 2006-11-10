//
// $Id$

package com.threerings.bang.game.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
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
import com.threerings.bang.game.data.card.Card;

import com.threerings.bang.ranch.client.UnitIcon;
import com.threerings.bang.ranch.client.UnitPalette;
import com.threerings.bang.ranch.client.UnitView;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.Item;
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
    public SelectionView (
        BangContext ctx, BangView view, BangConfig config,
        BangObject bangobj, int pidx)
    {
        super(ctx, "");

        _ctx = ctx;
        _view = view;
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

        _side = new BContainer(GroupLayout.makeVert(GroupLayout.TOP)) {
            protected Dimension computePreferredSize (int whint, int hhint) {
                Dimension d = super.computePreferredSize(whint, hhint);
                // be no wider than our unit view
                d.width = _uview.getPreferredSize(whint, hhint).width;
                return d;
            }
        };

        // add the bigshot info
        _side.add(_uname = new BLabel("", "pick_unit_name"));
        _uname.setFit(BLabel.Fit.SCALE);
        _side.add(_uview = new UnitView(ctx, true));
        _side.add(_utype = new BLabel("", "pick_team_choice"));

        // add a label for each selectable unit
        _team = new BLabel[_tconfigs.length];
        for (int ii = 0; ii < _team.length; ii++) {
            _side.add(_team[ii] = new BLabel("", "pick_team_choice"));
        }
        _contents.add(_side, BorderLayout.WEST);

        _center = new BContainer(new BorderLayout(0, 5));
        _contents.add(_center, BorderLayout.CENTER);

        // count up their cards and big shots
        int bscount = 0, cardcount = 0;
        for (Item item : _ctx.getUserObject().inventory) {
            if (item instanceof BigShotItem) {
                _bigShotId = item.getItemId();
                bscount++;
            } else if (item instanceof CardItem) {
                CardItem citem = (CardItem)item;
                Card card = Card.getCard(citem.getType());
                if (citem.getQuantity() > 0 && card != null &&
                    card.isPlayable(_bangobj)) {
                    cardcount++;
                }
            }
        }

        // add the steps across the top
        BContainer steps = GroupLayout.makeHBox(GroupLayout.CENTER);
        steps.add(new BLabel(_msgs.get("m.select_your"), "pick_subtitle"));
        _steps = new BLabel[STEPS.length];
        for (int ii = 0; ii < STEPS.length; ii++) {
            _steps[ii] = new BLabel(
                _msgs.get("m.select_" + STEPS[ii]), "pick_subtitle");
            // don't add the labels for steps we're going to skip
            if ((ii == BIGSHOT && bscount <= 1) ||
                (ii == CARDS && cardcount == 0)) {
                continue;
            }
            steps.add(_steps[ii]);
        }
        _center.add(steps, BorderLayout.NORTH);

        // and a tip along the bottom
        _center.add(_tip = new BLabel("", "pick_tip"), BorderLayout.SOUTH);

        // we need to create _ready here because code below is going to trigger
        // a call to updateBigShot()
        _buttons.add(_ready = new BButton(_msgs.get("m.next"),
                                          this, "picked_bigshot"));

        // create the big shot selection display
        _units = new UnitPalette(_ctx, _enabler, 4, 2);
        _units.setPaintBorder(true);
        _units.setStyleClass("pick_palette");
        _units.setUser(_ctx.getUserObject(), true);
        _units.selectFirstIcon();

        // if they have no big shots, skip that selection mode
        if (bscount > 1) {
            setStep(BIGSHOT, _units);
            updateBigShot();
        } else {
            setPickTeamMode();
        }
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent e)
    {
        String cmd = e.getAction();
        if (cmd.equals("picked_bigshot")) {
            UnitIcon icon = _units.getSelectedUnit();
            if (icon == null) {
                return;
            }
            _bigShotId = icon.getItemId();
            setPickTeamMode();

        } else if (cmd.equals("picked_team")) {
            // convert our team selection into an array
            ArrayList<String> units = new ArrayList<String>();
            for (int ii = 0; ii < _tconfigs.length; ii++) {
                if (_tconfigs[ii] != null) {
                    units.add(_tconfigs[ii].type);
                }
            }
            _tunits = units.toArray(new String[units.size()]);

            // if they have playable cards, let them pick some
            if (_steps[CARDS].isAdded()) {
                setPickCardsMode();
            } else {
                sendTeamSelection(new int[0]);
            }

        } else if (cmd.equals("picked_cards")) {
            // determine which cards are selected
            ArrayIntSet cardIds = new ArrayIntSet();
            for (int ii = 0; ii < GameCodes.MAX_CARDS; ii++) {
                CardItem item = _cards.getSelectedCard(ii);
                if (item != null) {
                    cardIds.add(item.getItemId());
                    // preload the card selection into our status view
                    _view.pstatus[_pidx].cardAdded(
                        Card.getCard(item.getType()), true);
                }
            }
            sendTeamSelection(cardIds.toIntArray());
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        _units.shutdown();
    }

    /**
     * Switches to the mode where we pick our teams.
     */
    protected void setPickTeamMode ()
    {
        // shutdown the bigshot selection palette
        _units.shutdown();

        // create the normal unit selection display
        _units = new UnitPalette(_ctx, _teamins, 4, 2);
        _units.setPaintBorder(true);
        _units.setStyleClass("pick_palette");
        _units.setSelectable(_team.length);
        _units.selectFirstIcon();

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

        _ready.setAction("picked_team");
        _ready.setText(_msgs.get("m.ready"));
        _ready.setEnabled(false);
        setStep(TEAM, _units);
    }

    /**
     * Switches to the mode where we pick our cards.
     */
    protected void setPickCardsMode ()
    {
        _ready.setAction("picked_cards");
        _cards = new CardPalette(_ctx, _bangobj);
        _cards.setStyleClass("pick_palette");
        setStep(CARDS, _cards);
    }

    protected void sendTeamSelection (int[] cards)
    {
        // don't allow anything to change while we're waiting
        _ready.setEnabled(false);

        // submit our selection to the server
        _bangobj.service.selectTeam(
            _ctx.getClient(), _bigShotId, _tunits, cards);

        // get ourselves out of the way
        _view.clearOverlay();
    }

    protected void updateBigShot ()
    {
        _ready.setEnabled(_units.getSelectedUnit() != null);
    }

    protected void updateTeam ()
    {
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

    protected void setStep (int step, BComponent content)
    {
        for (int ii = 0; ii < _steps.length; ii++) {
            _steps[ii].setEnabled(ii == step);
        }
        _tip.setText(_msgs.get("m.select_tip" + step));

        if (_stepcont != null) {
            _center.remove(_stepcont);
        }
        _stepcont = content;
        if (_stepcont != null) {
            _center.add(_stepcont, BorderLayout.CENTER);
        }
    }

    protected IconPalette.Inspector _enabler = new IconPalette.Inspector() {
        public void iconUpdated (SelectableIcon icon, boolean selected) {
            if (selected) {
                _uname.setText(icon.getText());
                UnitConfig conf = ((UnitIcon)icon).getUnit();
                _uview.setUnit(conf);
                _utype.setText(_ctx.xlate(GameCodes.GAME_MSGS, conf.getName()));
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
    protected BangView _view;
    protected MessageBundle _msgs;
    protected BangObject _bangobj;
    protected int _pidx;

    protected BContainer _side, _center;
    protected BComponent _stepcont;
    protected BLabel[] _steps;
    protected BLabel _uname, _utype, _tip;

    protected UnitView _uview;
    protected UnitPalette _units;
    protected CardPalette _cards;

    protected BLabel[] _team;
    protected UnitConfig[] _tconfigs;

    protected BButton _ready;

    protected int _bigShotId;
    protected String[] _tunits;

    protected static final String[] STEPS = { "bigshot", "team", "cards" };
    protected static final int BIGSHOT = 0;
    protected static final int TEAM = 1;
    protected static final int CARDS = 2;
}
