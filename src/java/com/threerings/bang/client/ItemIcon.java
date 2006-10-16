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
        this(ctx, item, false);
    }

    public ItemIcon (BasicContext ctx, Item item, boolean small)
    {
        _item = item;
        _small = small;
        setIcon(_item.createIcon(ctx, _item.getIconPath(small)));
        String text = _item.getName(small);
        if (text != null) {
            setText(ctx.xlate(BangCodes.BANG_MSGS, text));
        }
        String tt = _item.getTooltip();
        if (tt != null) {
            setTooltipText(ctx.xlate(BangCodes.BANG_MSGS, tt));
        }
    }

    /** Returns the item associated with this icon. */
    public Item getItem ()
    {
        return _item;
    }

    protected Item _item;
}
