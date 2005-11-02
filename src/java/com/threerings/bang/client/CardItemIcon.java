//
// $Id$

package com.threerings.bang.client;

import com.threerings.bang.client.BangUI;
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
        BangUI.configCardLabel(_label, card);
    }
}
