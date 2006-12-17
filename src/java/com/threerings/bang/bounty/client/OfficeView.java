//
// $Id$

package com.threerings.bang.bounty.client;

import com.jme.renderer.Renderer;
import com.jmex.bui.BButton;
import com.jmex.bui.BImage;
import com.jmex.bui.BLabel;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.ListUtil;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.bounty.data.BountyConfig;
import com.threerings.bang.bounty.data.OfficeCodes;

/**
 * Displays the Sheriff's Office user interface
 */
public class OfficeView extends ShopView
{
    public OfficeView (BangContext ctx, OfficeController ctrl)
    {
        super(ctx, OfficeCodes.OFFICE_MSGS);

        // add our various interface components
        add(new BLabel(_msgs.get("m.intro_tip"), "shop_status"),
            new Rectangle(232, 661, 570, 35));

        add(new WalletLabel(_ctx, true), new Rectangle(45, 40, 150, 40));
        add(createHelpButton(), new Point(780, 25));
        add(new TownButton(ctx), new Point(870, 25));
        add(_status = new StatusLabel(ctx), new Rectangle(250, 10, 520, 50));
        _status.setStyleClass("shop_status");

        // display the test bounty game interface for support/admins
        if (_ctx.getUserObject().tokens.isSupport()) {
            BButton test = new BButton(_msgs.get("m.bounty_test"), ctrl, "bounty_test");
            test.setStyleClass("alt_button");
            add(test, new Point(590, 630));
        }

        // do our hacky fake tab business
        _wantedTab = _ctx.loadImage("ui/office/tabs_wanted.png");

        for (int ii = 0; ii < TABS.length; ii++) {
            BButton btn = new BButton("", _selector, TABS[ii]);
            add(btn, new Rectangle(TAB_LOCS[ii], 623, 150, 30));
            btn.setStyleClass("invisibutton");
        }

        // create our two bounty list views
        _tabs = new BountyList[] {
            new BountyList(ctx, BountyConfig.Type.TOWN),
            new BountyList(ctx, BountyConfig.Type.MOST_WANTED)
        };
        add(_active = _tabs[0], TAB_CONTENT_RECT);

        // start with a random shop tip
        _status.setStatus(getShopTip(), false);
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
        _wantedTab.reference();
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _wantedTab.release();
    }

    @Override // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        // hackity hack hack hack
        if (_active == _tabs[1]) {
            _wantedTab.render(renderer, 179, 598, _alpha);
        }

        // render our children components over the fake tab
        super.renderComponent(renderer);
    }

    protected ActionListener _selector = new ActionListener() {
        public void actionPerformed (ActionEvent event) {
            int selidx = ListUtil.indexOf(TABS, event.getAction());
            remove(_active);
            add(_active = _tabs[selidx], TAB_CONTENT_RECT);
            BangUI.play(BangUI.FeedbackSound.TAB_SELECTED);
        }
    };

    protected StatusLabel _status;

    protected BImage _wantedTab;
    protected BountyList _active;
    protected BountyList[] _tabs;

    protected static Rectangle TAB_CONTENT_RECT = new Rectangle(43, 71, 508, 548);

    protected static final String[] TABS = { "town", "wanted" };
    protected static final int[] TAB_LOCS = { 200, 370, 540 };
}
