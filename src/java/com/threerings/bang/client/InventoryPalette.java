//
// $Id$

package com.threerings.bang.client;

import java.util.Iterator;

import com.jme.renderer.ColorRGBA;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.border.CompoundBorder;
import com.jmex.bui.border.EmptyBorder;
import com.jmex.bui.border.LineBorder;
import com.jmex.bui.layout.TableLayout;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Item;
import com.threerings.bang.util.BangContext;

/**
 * Displays the user's inventory.
 */
public class InventoryPalette extends BContainer
{
    public InventoryPalette (BangContext ctx)
    {
        super(new TableLayout(3, 5, 5));
        setBorder(new CompoundBorder(new LineBorder(ColorRGBA.black),
                                     new EmptyBorder(5, 5, 5, 5)));
        _ctx = ctx;
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // populate our item display every time we are shown as we may be
        // hidden, the player's inventory updated then reshown again
        int added = 0;
        PlayerObject user = _ctx.getUserObject();
        for (Iterator iter = user.inventory.iterator(); iter.hasNext(); ) {
            Item item = (Item)iter.next();
            ItemIcon icon = item.createIcon();
            if (icon == null) {
                continue;
            }
            icon.setItem(_ctx, item);
            add(icon);
            added++;
        }

        if (added == 0) {
            String msg = _ctx.xlate(BangCodes.BANG_MSGS, "m.status_noinventory");
            add(new BLabel(msg));
        }
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // clear out our item display
        removeAll();
    }

    protected BangContext _ctx;
}
