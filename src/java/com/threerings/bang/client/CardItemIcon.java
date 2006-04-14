//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.icon.ImageIcon;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.CardItem;
import com.threerings.bang.util.BasicContext;

/**
 * Displays a card inventory item.
 */
public class CardItemIcon extends ItemIcon
{
    @Override // documentation inherited
    protected void configureLabel (BasicContext ctx)
    {
        CardItem card = (CardItem)_item;
        String path = "cards/" + card.getType() + "/card.png";
        setIcon(new ImageIcon(ctx.loadImage(path)));
        String name = ctx.xlate(BangCodes.CARDS_MSGS, "m." + card.getType());
        setText(name + " x" + card.getQuantity());

        String msg = MessageBundle.compose("m.card_icon", "m." + card.getType(),
                                           "m." + card.getType() + "_tip");
        setTooltipText(ctx.xlate(BangCodes.CARDS_MSGS, msg));
    }
}
