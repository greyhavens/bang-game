//
// $Id$

package com.threerings.bang.bounty.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bounty.data.OfficeCodes;

/**
 * Displays the Sheriff's Office user interface
 */
public class OfficeView extends ShopView
{
    public OfficeView (BangContext ctx)
    {
        super(ctx, OfficeCodes.OFFICE_MSGS);

        // add our various interface components
        add(new BLabel(_msgs.get("m.intro_tip"), "shop_status"),
            new Rectangle(232, 661, 570, 35));

        String townId = _ctx.getUserObject().townId;
        add(new BLabel(_msgs.get("m.name_" + townId), "shopkeep_name_label"),
            new Rectangle(12, 513, 155, 25));

        add(new WalletLabel(_ctx, true), new Rectangle(25, 53, 150, 40));
        add(createHelpButton(), new Point(780, 25));
        add(new TownButton(ctx), new Point(870, 25));
        add(_status = new StatusLabel(ctx), new Rectangle(250, 10, 520, 50));
        _status.setStyleClass("shop_status");

        // start with a random shop tip
        _status.setStatus(getShopTip(), false);
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);
    }

    protected StatusLabel _status;
}
