//
// $Id$

package com.threerings.bang.avatar.client;

import com.jmex.bui.BLabel;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.BarberCodes;
import com.threerings.bang.avatar.data.BarberObject;

import static com.threerings.bang.Log.log;

/**
 * Displays the main barber interface wherein the player can create new "looks"
 * for their avatar and purchase them.
 */
public class BarberView extends ShopView
{
    public BarberView (BangContext ctx)
    {
        super(ctx, BarberCodes.BARBER_MSGS);

        add(new BLabel(_msgs.get("m.welcome"), "shop_status"),
            new Rectangle(203, 655, 610, 40));
        add(_status = new StatusLabel(ctx), new Rectangle(230, 10, 500, 50));

        // we need to handle displaying the avatar specially
        _avatar = new AvatarView(ctx);

        // put our new look and change clothes interfaces in tabs
        _newlook = new NewLookView(ctx, _status);
        _wearclothes = new WearClothingView(ctx, _status);

        // start with the new look view "selected"
        add(_newlook, CONTENT_RECT);

        add(new WalletLabel(ctx, true), new Rectangle(40, 37, 150, 35));
        add(createHelpButton(), new Point(745, 25));
        add(new TownButton(ctx), new Point(835, 25));
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);

        BarberObject barbobj = (BarberObject)plobj;
        _newlook.setBarberObject(barbobj);
        _wearclothes.setBarberObject(barbobj);
    }

    protected AvatarView _avatar;
    protected StatusLabel _status;
    protected NewLookView _newlook;
    protected WearClothingView _wearclothes;

    protected static Rectangle CONTENT_RECT = new Rectangle(40, 65, 980, 545);
}
