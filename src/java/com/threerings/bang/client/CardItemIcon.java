//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.icon.ImageIcon;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.util.BangContext;

/**
 * Displays a card inventory item.
 */
public class CardItemIcon extends ItemIcon
{
    protected void configureLabel (BangContext ctx)
    {
        CardItem card = (CardItem)_item;
        String path = "cards/" + card.getType() + "/card.png";
        setIcon(new ImageIcon(ctx.loadImage(path)));
        String name = ctx.xlate(BangCodes.CARDS_MSGS, "m." + card.getType());
        setText(name + " x" + card.getQuantity());
    }
}
