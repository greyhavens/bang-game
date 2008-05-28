//
// $Id$

package com.threerings.bang.game.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.jmex.bui.BCheckBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.Spacer;
import com.jmex.bui.background.ImageBackground;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;

import com.threerings.bang.client.BangPrefs;
import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.Item;
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
    public CardPalette (BangContext ctx, BangObject bangobj)
    {
        super(null, 4, 2, ItemIcon.ICON_SIZE, GameCodes.MAX_CARDS);
        setPaintBorder(true);

        _ctx = ctx;
        _bangobj = bangobj;
        _selfIdx = bangobj.getPlayerIndex(
            _ctx.getUserObject().getVisibleName());
        _small = BangPrefs.getCardPaletteSize();
        if (_cardBG == null) {
            _cardBG = new ImageIcon(_ctx.loadImage("ui/pregame/card_bg.png"));
            _foundBG = new ImageBackground(
                ImageBackground.CENTER_XY,
                _ctx.loadImage("ui/pstatus/card_found_up.png"));
        }

        BContainer selectable = new BContainer(GroupLayout.makeHStretch());
        String msg = _ctx.xlate(GameCodes.GAME_MSGS, "m.small_card_view");
        selectable.add(_smallView = new BCheckBox(msg), GroupLayout.FIXED);
        _smallView.addListener(_smallListener);
        _smallView.setSelected(_small);
        selectable.add(new Spacer(1, 1));
        remove(_bcont);
        selectable.add(_bcont, GroupLayout.FIXED);
        add(selectable, BorderLayout.SOUTH);

        // switch to the appropriate small or large view to start
        changeView();
    }

    public CardItem getSelectedCard (int index)
    {
        ItemIcon icon = (ItemIcon)getSelectedIcon(index);
        return icon == null ? null : (CardItem)icon.getItem();
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

    /**
     * Updates the size of the palette based on the checkbox value.
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
            init(16, 6, ItemIcon.SMALL_ICON_SIZE);
        } else {
            init(4, 2, ItemIcon.ICON_SIZE);
        }
        setPaintBackground(true);
        _iicont.add(_icont);

        // find cards held over from last round; we could try to do some of
        // this once instead of every time we switch modes, but it would make
        // things more complicated and this isn't very expensive
        PlayerObject user = _ctx.getUserObject();
        ArrayList<CardItem> carditems = new ArrayList<CardItem>();
        ArrayList<Card> reservedCards = new ArrayList<Card>();
        for (Card card : _bangobj.cards) {
            if (card.owner == _selfIdx && card.found == false) {
                reservedCards.add(card);
            }
        }

        for (Item item : user.inventory) {
            if (item instanceof CardItem) {
                CardItem citem = (CardItem)item;
                Card card = Card.getCard(citem.getType());
                if (card == null) {
                    continue;
                }
                // update the count based on the reserved cards
                for (Card reserved : reservedCards) {
                    if (card.getType().equals(reserved.getType())) {
                        citem = (CardItem)citem.clone();
                        citem.playCard();
                    }
                }
                if (card.isPlayable(_bangobj, user.townId) && citem.getQuantity() > 0) {
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
            } else {
                iicon.setFitted(true);
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

    protected BCheckBox _smallView;
    protected boolean _small;

    protected static ImageIcon _cardBG;
    protected static ImageBackground _foundBG;
}
