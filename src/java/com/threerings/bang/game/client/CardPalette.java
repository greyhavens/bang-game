//
// $Id$

package com.threerings.bang.game.client;

import java.util.Iterator;

import com.jmex.bui.BLabel;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.GameCodes;

/**
 * Displays all card items held by the player and allows them to select a
 * subset of them for use in a game.
 */
public class CardPalette extends IconPalette
{
    public static class CardIcon extends SelectableIcon
    {
        public CardItem item;

        public CardIcon (CardItem item)
        {
            this.item = item;
            BangUI.configCardLabel(_label, item);
        }
    }

    public CardPalette (BangContext ctx, BangObject bangobj)
    {
        super(null, 4, GameCodes.MAX_CARDS);

        int added = 0;
        PlayerObject user = ctx.getUserObject();
        for (Iterator iter = user.inventory.iterator(); iter.hasNext(); ) {
            Object item = iter.next();
            if (item instanceof CardItem) {
                add(new CardIcon((CardItem)item));
                added++;
            }
        }

        if (added == 0) {
            String msg = ctx.xlate(GameCodes.GAME_MSGS, "m.select_nocards");
            add(new BLabel(msg));
        }

        // reduce the number of selectable cards by the number we have waiting
        // to be played
        _selectable -= bangobj.countPlayerCards(
            bangobj.getPlayerIndex(user.getVisibleName()));
    }

    public CardItem getSelectedCard (int index)
    {
        CardIcon icon = (CardIcon)getSelectedIcon(index);
        return icon == null ? null : icon.item;
    }
}
