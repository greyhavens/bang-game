//
// $Id$

package com.threerings.bang.game.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.jmex.bui.BCheckBox;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.background.ImageBackground;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.game.data.card.Card;

/**
 * Displays all card items held by the player and allows them to select a
 * subset of them for use in a game.
 */
public class CardPalette extends IconPalette
{
    public CardPalette (BangContext ctx, BangController ctrl,
                        BangObject bangobj, BLabel[] selcards)
    {
        super(null, 4, 1, ItemIcon.ICON_SIZE, GameCodes.MAX_CARDS);
        setPaintBorder(true);

        _ctx = ctx;
        _bangobj = bangobj;
        _selcards = selcards;
        _selfIdx = bangobj.getPlayerIndex(
            _ctx.getUserObject().getVisibleName());
        _small = BangPrefs.getCardPaletteSize();
        if (_cardBG == null) {
            _cardBG = new ImageIcon(_ctx.loadImage("ui/pregame/card_bg.png"));
            _foundBG = new ImageBackground(ImageBackground.CENTER_XY, 
                    _ctx.loadImage("ui/pstatus/card_found_up.png"));
        }

        BContainer selectable = new BContainer(GroupLayout.makeHStretch());
        selectable.add(_smallView = new BCheckBox(_ctx.xlate(
                GameCodes.GAME_MSGS, "m.small_card_view")), GroupLayout.FIXED);
        _smallView.addListener(_smallListener);
        _smallView.setSelected(_small);
        selectable.add(new Spacer(1, 1));
        remove(_bcont);
        selectable.add(_bcont, GroupLayout.FIXED);
        add(selectable, BorderLayout.SOUTH);
        changeView();
    }

    @Override // documentation inherited
    public void setPaintBackground (boolean paintbg)
    {
        if (_small) {
            _icont.setStyleClass(paintbg ? "small_palette_background" : null);
        } else {
            super.setPaintBackground(paintbg);
        }
    }

    public CardItem getSelectedCard (int index)
    {
        ItemIcon icon = (ItemIcon)getSelectedIcon(index);
        return icon == null ? null : (CardItem)icon.getItem();
    }

    protected void iconUpdated (SelectableIcon icon, boolean selected)
    {
        super.iconUpdated(icon, selected);

        // stop updating once we've been "disabled"
        if (_selectable > 0) {
            updateSelections();
        }
    }

    protected void updateSelections ()
    {
        // start with the cards already in the game object
        int iconidx = 0;
        for (Iterator iter = _bangobj.cards.iterator(); iter.hasNext(); ) {
            Card card = (Card)iter.next();
            if (card.owner != _selfIdx) {
                continue;
            }
            if (card.found) {
                _selcards[iconidx].setBackground(BComponent.DEFAULT, _foundBG);
                _selcards[iconidx].setBackground(BComponent.HOVER, null);
            }
            _selcards[iconidx++].setIcon(makeIcon(card));
        }

        // now add selected cards (iterate over _icons rather that _selections
        // so that we match the visual order of the selected icons)
        for (SelectableIcon icon : _icons) {
            if (!icon.isSelected()) {
                continue;
            }
            CardItem card = (CardItem)((ItemIcon)icon).getItem();
            _selcards[iconidx++].setIcon(makeIcon(card.getCard()));
        }

        // finally clear out the remaining icons
        for (int ii = iconidx; ii < _selcards.length; ii++) {
            _selcards[ii].setIcon(_cardBG);
        }
    }

    protected ImageIcon makeIcon (Card card)
    {
        return new ImageIcon(_ctx.loadImage(card.getIconPath("icon")));
    }

    /**
     * Updated the size of the palette based on the checkbox value.
     */
    protected void changeView ()
    {
        _small = _smallView.isSelected();
        BangPrefs.updateCardPaletteSize(_small);
        ArrayList<SelectableIcon> selected = 
            new ArrayList<SelectableIcon>(_selections);
        clear();
        _iicont.remove(_icont);
        if (_small) {
            init(16, 3, ItemIcon.SMALL_ICON_SIZE);
        } else {
            init(4, 1, ItemIcon.ICON_SIZE);
        }
        setPaintBackground(true);
        _iicont.add(_icont);
        PlayerObject user = _ctx.getUserObject();
        ArrayList<CardItem> carditems = new ArrayList<CardItem>();

        // find cards held over from last round
        ArrayList<Card> _reservedCards = new ArrayList<Card>();
        for (Iterator iter = _bangobj.cards.iterator(); iter.hasNext(); ) {
            Card card = (Card)iter.next();
            if (card.owner == _selfIdx && card.found == false) {
                _reservedCards.add(card);
            }
        }

        for (Iterator iter = user.inventory.iterator(); iter.hasNext(); ) {
            Object item = iter.next();
            if (item instanceof CardItem) {
                CardItem citem = (CardItem)item;
                Card card = Card.getCard(citem.getType());
                // Update the count based on the reserved cards
                for (Card reserved : _reservedCards) {
                    if (card.getType().equals(reserved.getType())) {
                        citem = (CardItem)citem.clone();
                        citem.playCard();
                    }
                }
                if (card != null && card.isPlayable(_bangobj) &&
                    citem.getQuantity() > 0) {
                    carditems.add(citem);
                }
            }
        }
        Collections.sort(carditems, new Comparator<CardItem>() {
            public int compare (CardItem c1, CardItem c2) {
                return c1.getType().compareTo(c2.getType());
            }
        });
        for (CardItem citem : carditems) {
            ItemIcon iicon = new ItemIcon(_ctx, citem, _small);
            if (_small) {
                iicon.setStyleClass("card_palette_icon");
            }
            addIcon(iicon);
            for (SelectableIcon icon : selected) {
                if (citem == ((ItemIcon)icon).getItem()) {
                    iicon.setSelected(true);
                    iconUpdated(iicon, true);
                }
            }
        }

        // reduce the number of selectable cards by the number we have waiting
        // to be played
        _selectable -= _bangobj.countPlayerCards(
            _bangobj.getPlayerIndex(user.getVisibleName()));

        updateSelections();
    }

    protected ActionListener _smallListener = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            if (_small != _smallView.isSelected()) {
                changeView();
            }
        }
    };

    protected BangContext _ctx;
    protected BangObject _bangobj;
    protected int _selfIdx;
    protected BLabel[] _selcards;
    protected BCheckBox _smallView;
    protected boolean _small;
    protected static ImageIcon _cardBG;
    protected static ImageBackground _foundBG;
}
