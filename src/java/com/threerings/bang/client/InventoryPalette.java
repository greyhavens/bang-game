//
// $Id$

package com.threerings.bang.client;

import java.util.Iterator;

import com.samskivert.util.Predicate;

import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Item;
import com.threerings.bang.util.BangContext;

/**
 * Displays some subset of the user's inventory.
 */
public class InventoryPalette extends IconPalette
{
    /**
     * Creates an {@link InventoryPalette} with 5 columns, 3 rows,
     * and no {@link Inspector}.
     */
    public InventoryPalette (BangContext ctx, Predicate<Item> itemp)
    {
        this(ctx, itemp, 5, 3);
    }

    /**
     * Creates an {@link InventoryPalette} without {@link Inspector}.
     */
    public InventoryPalette (BangContext ctx, Predicate<Item> itemp,
                             int columns, int rows)
    {
        this(ctx, itemp, null, columns, rows);
    }

    /**
     * Creates an {@link InventoryPalette}.
     */
    public InventoryPalette (BangContext ctx, Predicate<Item> itemp,
                             Inspector inspector, int columns, int rows)
    {
        super(inspector, columns, rows, ItemIcon.ICON_SIZE, 0);
        setPaintBackground(true);
        _ctx = ctx;
        _itemp = itemp;
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // populate our item display every time we are shown as we may be
        // hidden, the player's inventory updated, then reshown again
        int added = 0;
        PlayerObject user = _ctx.getUserObject();
        for (Iterator iter = user.inventory.iterator(); iter.hasNext(); ) {
            Item item = (Item)iter.next();
            if (!_itemp.isMatch(item)) {
                continue;
            }
            addIcon(new ItemIcon(_ctx, item));
            added++;
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // clear out our item display
        clear();
    }

    protected BangContext _ctx;
    protected Predicate<Item> _itemp;
}
