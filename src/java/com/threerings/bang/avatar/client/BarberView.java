//
// $Id$

package com.threerings.bang.avatar.client;

import com.jme.renderer.Renderer;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.data.BarberCodes;
import com.threerings.bang.avatar.data.BarberObject;

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
        add(_tip = new BLabel(_msgs.get("m.looks_tip"), "barber_tip_label"),
            new Rectangle(185, 565, 390, 55));

        add(_status = new StatusLabel(ctx), new Rectangle(230, 10, 500, 50));
        _status.setStyleClass("shop_status");
        _status.setText(getShopTip());

        // we need to handle displaying the avatar specially
        _avatar = new AvatarView(ctx, 2, true, false);

        // put our new look and change clothes interfaces in tabs
        _wearclothes = new WearClothingView(ctx, _status);
        _newlook = new NewLookView(ctx, _status);
        _editchar = new EditCharacterView(ctx, _status);

        // start with the wear clothes view "selected"
        add(_active = _wearclothes, CONTENT_RECT);

        // do our hacky fake tab business
        _looktab = _ctx.loadImage("ui/barber/top_looks.png");
        _chartab = _ctx.loadImage("ui/barber/top_character.png");

        for (int ii = 0; ii < TABS.length; ii++) {
            BButton btn = new BButton("", _selector, TABS[ii]);
            add(btn, new Rectangle(TAB_LOCS[ii], 623, 150, 30));
            btn.setStyleClass("def_button");
        }

        add(new WalletLabel(ctx, true), new Rectangle(40, 38, 150, 40));
        add(createHelpButton(), new Point(745, 25));
        add(new TownButton(ctx), new Point(835, 25));
    }

    @Override // documentation inherited
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);

        BarberObject barbobj = (BarberObject)plobj;
        _wearclothes.setBarberObject(barbobj);
        _newlook.setBarberObject(barbobj);
        _editchar.setBarberObject(barbobj);
    }

    @Override // documentation inherited
    protected Point getShopkeepNameLocation ()
    {
        return new Point(23, 548);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _looktab.reference();
        _chartab.reference();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _looktab.release();
        _chartab.release();
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        // hackity hack hack hack
        if (_active == _newlook) {
            _looktab.render(renderer, 179, 598, _alpha);
        } else if (_active == _editchar) {
            _chartab.render(renderer, 179, 598, _alpha);
        }

        // render our children components over the fake tab
        super.renderComponent(renderer);
    }

    protected ActionListener _selector = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            BComponent nactive;
            String action = event.getAction(), tip;
            if (action.equals("looks")) {
                nactive = _newlook;
                tip = "looks";
            } else if (action.equals("clothes")) {
                nactive = _wearclothes;
                tip = "clothes";
            } else { // if (action.equals("character")) {
                nactive = _editchar;
                tip = "character";
            }

            remove(_active);
            add(_active = nactive, CONTENT_RECT);
            _tip.setText(_msgs.get("m." + tip + "_tip"));
            BangUI.play(BangUI.FeedbackSound.TAB_SELECTED);
        }
    };

    protected AvatarView _avatar;
    protected BComponent _active;
    protected BLabel _tip;
    protected StatusLabel _status;
    protected BImage _looktab, _chartab;

    protected WearClothingView _wearclothes;
    protected NewLookView _newlook;
    protected EditCharacterView _editchar;

    protected static Rectangle CONTENT_RECT = new Rectangle(40, 65, 980, 545);

    protected static final String[] TABS = { "clothes", "looks", "character" };
    protected static final int[] TAB_LOCS = { 200, 370, 540 };
}
