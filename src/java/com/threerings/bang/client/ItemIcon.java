//
// $Id$

package com.threerings.bang.client;

import com.threerings.bang.client.bui.PaletteIcon;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Item;
import com.threerings.bang.util.BasicContext;

/**
 * Displays an icon and descriptive text for a particular inventory item.
 */
public class ItemIcon extends PaletteIcon
{
    public ItemIcon (BasicContext ctx, Item item)
    {
        _item = item;
        setIcon(_item.createIcon(ctx, _item.getIconPath()));
        setText(ctx.xlate(BangCodes.BANG_MSGS, _item.getName()));
        setTooltipText(ctx.xlate(BangCodes.BANG_MSGS, _item.getTooltip()));
    }

    /** Returns the item associated with this icon. */
    public Item getItem ()
    {
        return _item;
    }

    protected Item _item;
}
