//
// $Id$

package com.threerings.bang.client;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import com.samskivert.util.Predicate;

import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.data.Article;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
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
        this(ctx, itemp, COLUMNS, 3);
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
        populate();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // clear out our item display
        clear();
    }

    /**
     * Populates the palette with icons based on the contents of the players
     * inventory.
     */
    protected void populate ()
    {
        PlayerObject user = _ctx.getUserObject();
        Item[] items = user.inventory.toArray(new Item[user.inventory.size()]);
        Arrays.sort(items, new ItemComparator());
        // sort the items in some vaguely sensible order
        for (Item item : items) {
            if (!_itemp.isMatch(item)) {
                continue;
            }
            addIcon(new ItemIcon(_ctx, item));
        }
    }

    /** Used to sort the inventory display. */
    protected class ItemComparator implements Comparator<Item> {
        public int compare (Item one, Item two) {
            if (!one.getClass().equals(two.getClass())) {
                return one.getClass().getName().compareTo(
                    two.getClass().getName());
            }
            // compare articles specially to make Rick happy
            if (one instanceof Article) {
                return Article.ARTICLE_COMP.compare((Article)one, (Article)two);
            } else {
                String t1 = _ctx.xlate(BangCodes.BANG_MSGS, one.getName(false));
                return t1.compareTo(
                    _ctx.xlate(BangCodes.BANG_MSGS, two.getName(false)));
            }
        }
    };

    protected BangContext _ctx;
    protected Predicate<Item> _itemp;

    protected static final int COLUMNS = 5;
}
