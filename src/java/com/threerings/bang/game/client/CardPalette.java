//
// $Id$

package com.threerings.bang.game.client;

import java.util.Iterator;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;

/**
 * Displays all card items held by the player and allows them to select a
 * subset of them for use in a game.
 */
public class CardPalette extends IconPalette
{
    public CardPalette (BangContext ctx, BangObject bangobj)
    {
        super(null, 4, 1, ItemIcon.ICON_SIZE, GameCodes.MAX_CARDS, true);

        PlayerObject user = ctx.getUserObject();
        for (Iterator iter = user.inventory.iterator(); iter.hasNext(); ) {
            Object item = iter.next();
            if (item instanceof CardItem) {
                CardItem card = (CardItem)item;
                ItemIcon icon = card.createIcon();
                icon.setItem(ctx, card);
                addIcon(icon);
            }
        }

        // reduce the number of selectable cards by the number we have waiting
        // to be played
        _selectable -= bangobj.countPlayerCards(
            bangobj.getPlayerIndex(user.getVisibleName()));
    }

    public CardItem getSelectedCard (int index)
    {
        ItemIcon icon = (ItemIcon)getSelectedIcon(index);
        return icon == null ? null : (CardItem)icon.getItem();
    }
}
