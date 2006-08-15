//
// $Id$

package com.threerings.bang.game.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.jmex.bui.BLabel;
import com.jmex.bui.icon.ImageIcon;

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
        setPaintBackground(true);
        setPaintBorder(true);

        _ctx = ctx;
        _bangobj = bangobj;
        _selcards = selcards;
        _selfIdx = bangobj.getPlayerIndex(
            _ctx.getUserObject().getVisibleName());

        PlayerObject user = ctx.getUserObject();
        ArrayList<CardItem> carditems = new ArrayList<CardItem>();
        for (Iterator iter = user.inventory.iterator(); iter.hasNext(); ) {
            Object item = iter.next();
            if (item instanceof CardItem) {
                CardItem citem = (CardItem)item;
                Card card = Card.getCard(citem.getType());
                if (card != null && card.isPlayable(bangobj)) {
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
            addIcon(new ItemIcon(ctx, citem));
        }

        // reduce the number of selectable cards by the number we have waiting
        // to be played
        _selectable -= bangobj.countPlayerCards(
            bangobj.getPlayerIndex(user.getVisibleName()));

        updateSelections();
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
            _selcards[ii].setIcon(null);
        }
    }

    protected ImageIcon makeIcon (Card card)
    {
        return new ImageIcon(_ctx.loadImage(card.getIconPath("icon")));
    }

    protected BangContext _ctx;
    protected BangObject _bangobj;
    protected int _selfIdx;
    protected BLabel[] _selcards;
}
