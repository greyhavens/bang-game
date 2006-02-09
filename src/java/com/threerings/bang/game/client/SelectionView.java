//
// $Id$

package com.threerings.bang.game.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;
import com.samskivert.util.ArrayIntSet;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.game.data.BangConfig;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;

import com.threerings.bang.ranch.client.UnitIcon;
import com.threerings.bang.ranch.client.UnitPalette;
import com.threerings.bang.ranch.client.UnitView;

import com.threerings.bang.data.CardItem;
import com.threerings.bang.util.BangContext;

/**
 * Displays an interface for selecting a big shot and a starting hand of
 * cards from a player's inventory.
 */
public class SelectionView extends BDecoratedWindow
    implements ActionListener
{
    /**
     * Creates a nice header to display on pre-game dialogs.
     */
    public static BContainer createRoundHeader (
        BangContext ctx, BangConfig config, BangObject bangobj)
    {
        BContainer header = GroupLayout.makeHBox(GroupLayout.CENTER);
        String msg = MessageBundle.compose(
            "m.round_header",
            MessageBundle.taint(String.valueOf((bangobj.roundId + 1))),
            "m.scenario_" + bangobj.scenarioId,
            MessageBundle.taint(bangobj.boardName));
        header.add(
            new BLabel(ctx.xlate(GameCodes.GAME_MSGS, msg), "scroll_title"));
        return header;
    }

    public SelectionView (BangContext ctx, BangConfig config,
                          BangObject bangobj, int pidx)
    {
        super(ctx.getStyleSheet(), null);

        _ctx = ctx;
        _msgs = _ctx.getMessageManager().getBundle(GameCodes.GAME_MSGS);
        _bangobj = bangobj;
        _pidx = pidx;

        setLayoutManager(new BorderLayout(25, 15));
        add(createRoundHeader(ctx, config, bangobj), BorderLayout.NORTH);
        
        BContainer side = GroupLayout.makeVBox(GroupLayout.TOP);
        add(side, BorderLayout.WEST);
        side.add(_uname = new BLabel("", "pick_unit_name"));
        side.add(_uview = new UnitView(ctx, true));

        BContainer cards = GroupLayout.makeHBox(GroupLayout.CENTER);
        for (int ii = 0; ii < _cardsels.length; ii++) {
            cards.add(_cardsels[ii] = new BLabel("", "card_icon"));
        }
        side.add(cards);

        BContainer cent = GroupLayout.makeVBox(GroupLayout.TOP);
        ((GroupLayout)cent.getLayoutManager()).setOffAxisJustification(
            GroupLayout.LEFT);
        add(cent, BorderLayout.CENTER);

        // create the big shot selection display
        cent.add(new BLabel(_msgs.get("m.select_bigshot"), "pick_label"));
        _units = new UnitPalette(ctx, _enabler, 4, 1);
        _units.setPaintBorder(true);
        _units.setStyleClass("pick_palette");
        _units.setUser(_ctx.getUserObject());
        _units.selectFirstIcon();
        cent.add(_units);

        // create the card selection display
        cent.add(new BLabel(_msgs.get("m.select_cards"), "pick_label"));
        cent.add(_cards = new CardPalette(ctx, bangobj, _cardsels));
        _cards.setStyleClass("pick_palette");

        BContainer footer = GroupLayout.makeHBox(GroupLayout.CENTER);
        footer.add(_ready = new BButton(_msgs.get("m.ready"), this, "ready"));
        add(footer, BorderLayout.SOUTH);

        updateReady();
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent e)
    {
        UnitIcon icon = _units.getSelectedUnit();
        if (icon == null) {
            return;
        }

        // don't allow double clickage
        _ready.setEnabled(false);
        // prevent reenabling by a click on the big shot selection palette
        _ready = null;

        // determine which cards are selected
        ArrayIntSet cardIds = new ArrayIntSet();
        for (int ii = 0; ii < GameCodes.MAX_CARDS; ii++) {
            CardItem item = _cards.getSelectedCard(ii);
            if (item != null) {
                cardIds.add(item.getItemId());
            }
        }

        int bigShotId = icon.getItemId();
        _bangobj.service.selectStarters(
            _ctx.getClient(), bigShotId, cardIds.toIntArray());
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _units.shutdown();
    }

    protected void updateReady ()
    {
        if (_ready != null) {
            _ready.setEnabled(_units.getSelectedUnit() != null);
        }
    }

    protected IconPalette.Inspector _enabler = new IconPalette.Inspector() {
        public void iconSelected (SelectableIcon icon) {
            _uname.setText(icon.getText());
            _uview.setUnit(((UnitIcon)icon).getUnit());
            updateReady();
        }
        public void selectionCleared () {
            updateReady();
        }
    };

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected BangObject _bangobj;
    protected int _pidx;

    protected BLabel _uname;
    protected UnitView _uview;
    protected UnitPalette _units;

    protected CardPalette _cards;
    protected BLabel[] _cardsels = new BLabel[GameCodes.MAX_CARDS];

    protected BButton _ready;
}
