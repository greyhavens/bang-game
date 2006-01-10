//
// $Id$

package com.threerings.bang.client;

import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.util.Dimension;

import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.Item;
import com.threerings.bang.util.BangContext;

/**
 * Displays an icon and descriptive text for a particular inventory item.
 */
public class ItemIcon extends SelectableIcon
{
    public ItemIcon ()
    {
        setOrientation(VERTICAL);
    }

    /** Returns the item associated with this icon. */
    public Item getItem ()
    {
        return _item;
    }

    /** Configures this icon with its associated item. */
    public ItemIcon setItem (BangContext ctx, Item item)
    {
        _item = item;
        configureLabel(ctx);
        return this;
    }

    @Override // documentation inherited
    public Dimension getPreferredSize (int whint, int hhint)
    {
        return new Dimension(128, 143);
    }

    protected void configureLabel (BangContext ctx)
    {
        setIcon(new ImageIcon(ctx.loadImage("ui/unknown_item.png")));
        setText(_item.toString());
    }

    protected Item _item;
}
