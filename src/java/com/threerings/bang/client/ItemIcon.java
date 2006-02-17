//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.util.Dimension;

import com.threerings.bang.client.bui.PaletteIcon;
import com.threerings.bang.data.Item;
import com.threerings.bang.util.BasicContext;

/**
 * Displays an icon and descriptive text for a particular inventory item.
 */
public class ItemIcon extends PaletteIcon
{
    public ItemIcon ()
    {
    }

    /** Returns the item associated with this icon. */
    public Item getItem ()
    {
        return _item;
    }

    /** Configures this icon with its associated item. */
    public ItemIcon setItem (BasicContext ctx, Item item)
    {
        _item = item;
        configureLabel(ctx);
        return this;
    }

    protected void configureLabel (BasicContext ctx)
    {
        setIcon(new ImageIcon(ctx.loadImage("ui/icons/unknown_item.png")));
        setText(_item.toString());
    }

    protected Item _item;
}
