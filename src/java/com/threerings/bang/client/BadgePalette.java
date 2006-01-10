//
// $Id$

package com.threerings.bang.client;

import java.util.Iterator;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.layout.TableLayout;

import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

/**
 * Displays the user's collected bages.
 */
public class BadgePalette extends BContainer
{
    public BadgePalette (BangContext ctx)
    {
        super(new TableLayout(4, 5, 5));
        setStyleClass("padded_box");
        _ctx = ctx;
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // populate our item display every time we are shown as we may be
        // hidden, the player's badges updated, then reshown again
        int added = 0;
        PlayerObject user = _ctx.getUserObject();
        for (Iterator iter = user.inventory.iterator(); iter.hasNext(); ) {
            Item item = (Item)iter.next();
            if (!(item instanceof Badge)) {
                continue;
            }

            ItemIcon icon = item.createIcon();
            if (icon == null) {
                continue;
            }
            icon.setItem(_ctx, item);
            add(icon);
            added++;
        }

        if (added == 0) {
            String msg = _ctx.xlate(BangCodes.BANG_MSGS, "m.status_empty");
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
