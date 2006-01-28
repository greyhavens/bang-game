//
// $Id$

package com.threerings.bang.client;

import java.util.Iterator;

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
    public static interface Predicate
    {
        public boolean includeItem (Item item);
    }

    public InventoryPalette (BangContext ctx, Predicate itemp)
    {
        super(null, 5, 3, ItemIcon.ICON_SIZE, 0, true);
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
            if (!_itemp.includeItem(item)) {
                continue;
            }

            ItemIcon icon = item.createIcon();
            if (icon == null) {
                continue;
            }
            icon.setItem(_ctx, item);
            addIcon(icon);
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
    protected Predicate _itemp;
}
