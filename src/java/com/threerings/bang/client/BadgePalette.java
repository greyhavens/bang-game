//
// $Id$

package com.threerings.bang.client;

import java.util.HashMap;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

/**
 * Displays a player's earned badges.
 */
public class BadgePalette extends InventoryPalette
{
    public BadgePalette (BangContext ctx)
    {
        super(ctx, null, false);
    }

    @Override // from InventoryPalette
    protected void populate ()
    {
        PlayerObject user = _ctx.getUserObject();
        HashMap<Badge.Type,Badge> badges = new HashMap<Badge.Type,Badge>();
        for (Item item : user.inventory) {
            if (item instanceof Badge) {
                Badge badge = (Badge)item;
                badges.put(badge.getType(), badge);
            }
        }

        // lay the badges out in the pre-defined order
        int count = 0;
        Badge[] row = new Badge[COLUMNS];
        for (int ii = 0; ii < Badge.LAYOUT.length; ii++) {
            int column = ii % COLUMNS;
            if (column == 0) {
                count = 0;
            }

            Badge.Type type = Badge.LAYOUT[ii];
            Badge badge = badges.get(type);
            if (badge != null) {
                row[column] = badge;
                count++;
            } else if (type != null) {
                row[column] = type.newBadge();
                row[column].setSilhouette(true);
            } else {
                row[column] = Badge.Type.UNUSED.newBadge();
                row[column].setSilhouette(true);
            }

            // if we have a badge from this row, or it's one of the first six
            // rows, show the whole row
            if (column == COLUMNS-1 && (count > 0 || (ii/COLUMNS) < 6)) {
                for (int jj = 0; jj < COLUMNS; jj++) {
                    addIcon(new ItemIcon(_ctx, row[jj]));
                }
            }
        }
    }
}
