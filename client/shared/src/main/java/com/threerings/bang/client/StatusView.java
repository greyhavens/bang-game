//
// $Id$

package com.threerings.bang.client;

import com.badlogic.gdx.Input.Keys;

import com.jmex.bui.BButton;
import com.jmex.bui.BComponent;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.Predicate;

import com.threerings.util.MessageBundle;

import com.threerings.bang.avatar.client.CreateAvatarView;
import com.threerings.bang.avatar.client.PickLookView;
import com.threerings.bang.ranch.client.UnitPalette;

import com.threerings.bang.data.Article;
import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;

import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.client.bui.PaletteIcon;
import com.threerings.bang.util.BangContext;

/**
 * Displays a summary of a player's status, including: cash on hand,
 * inventory items, big shots.
 */
public class StatusView extends BWindow
    implements ActionListener
{
    /** The index of the items tab. */
    public static final int ITEMS_TAB = 0;

    /** The index of the Big Shots tab. */
    public static final int BIGSHOTS_TAB = 1;

    /** The index of the badges tab. */
    public static final int BADGES_TAB = 2;

    /** The index of the duds tab. */
    public static final int DUDS_TAB = 3;

    /** The index of the pardners tab. */
    public static final int PARDNERS_TAB = 4;

    /**
     * Binds global key commands that popup the status view when particular
     * keys are pressed.
     */
    public static void bindKeys (final BangContext ctx)
    {
        GlobalKeyManager.Command showStatus = new GlobalKeyManager.Command() {
            public void invoke (int keyCode, int modifiers) {
                // determine which tab we want to show
                int tabidx = 0;
                for (int ii = 0; ii < STATUS_KEYMAP.length; ii += 2) {
                    if (STATUS_KEYMAP[ii] == keyCode) {
                        tabidx = STATUS_KEYMAP[ii+1];
                        break;
                    }
                }
                showStatusTab(ctx, tabidx);
            }
        };

        for (int ii = 0; ii < STATUS_KEYMAP.length; ii += 2) {
            ctx.getKeyManager().registerCommand(STATUS_KEYMAP[ii], showStatus);
        }
    }

    /**
     * Displays the player status view with the specified tab selected.
     */
    public static void showStatusTab (BangContext ctx, int tabidx)
    {
        // determine whether we can pop up the status view right now
        boolean canShow = ctx.getBangClient().canDisplayPopup(
            MainView.Type.STATUS);

        // get the status view from the client; if it is not already
        // showing we will only create it if we're allowed
        StatusView status = ctx.getBangClient().getStatusView(canShow);
        if (status == null) {
            return;
        }

        if (status.isAdded()) {
            // ignore key strokes if we're not the top window
            if (ctx.getRootNode().isOnTop(status)) {
                if (tabidx == status.getSelectedTab()) {
                    ctx.getBangClient().clearPopup(status, true);
                } else {
                    status.setSelectedTab(tabidx);
                }
            }
        } else if (canShow) {
            status.setSelectedTab(tabidx);
            ctx.getBangClient().displayPopup(status, true);
        }
    }

    /**
     * Clears the status view key bindings.
     */
    public static void clearKeys (BangContext ctx)
    {
        for (int ii = 0; ii < STATUS_KEYMAP.length; ii += 2) {
            ctx.getKeyManager().clearCommand(STATUS_KEYMAP[ii]);
        }
    }

    public StatusView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), new AbsoluteLayout());
        setStyleClass("status_view");

        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        setModal(true);

        PlayerObject user = ctx.getUserObject();
        add(_handle = new BLabel("", "status_handle"),
            new Rectangle(40, 590, 195, 33));
        _handle.setFit(BLabel.Fit.SCALE);
        String poster = user.hasCharacter() ? "poster" : "avatar";
        _posterBtn = new BButton(_msgs.get("m.status_" + poster), this, poster);
        _posterBtn.setStyleClass("big_button");
        add(_posterBtn, new Point(40, 147));

        add(new PickLookView(ctx, false), new Point(10, 231));
        add(new WalletLabel(ctx, true), new Rectangle(77, 66, 150, 40));
        add(_tabinfo = new BLabel("", "status_tabinfo"),
            new Rectangle(290, 55, 453, 40));

        BContainer btns = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout)btns.getLayoutManager()).setGap(15);
        btns.add(new BButton(_msgs.get("m.status_to_town"), this, "to_town"));
        btns.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"));
        add(btns, new Rectangle(652, 8, 310, 35));

        add(_tabs = new HackyTabs(ctx, false, "ui/status/tab_", TABS, 137, 17) {
            protected void tabSelected (int index) {
                StatusView.this.selectTab(index);
            }
        }, new Rectangle(265, 575, 15+5*140, 66));

        // start with the inventory tab selected
        selectTab(_selectedTab);
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if (cmd.equals("to_town")) {
            if (_ctx.getBangClient().showTownView()) {
                _ctx.getBangClient().clearPopup(this, true);
            }

        } else if (cmd.equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);

        } else if (cmd.equals("poster")) {
            WantedPosterView.displayWantedPoster(_ctx, _ctx.getUserObject().handle);
            _posterBtn.setEnabled(false);

        } else if (cmd.equals("avatar")) {
            _ctx.getBangClient().clearPopup(this, true);
            CreateAvatarView.show(_ctx);
        }
    }

    /**
     * Returns the currently selected tab.
     */
    public int getSelectedTab ()
    {
        return _selectedTab;
    }

    /**
     * Selects the specified tab.
     */
    public void setSelectedTab (int tabidx)
    {
        _tabs.selectTab(tabidx);
    }

    @Override // from BComponent
    protected void wasAdded ()
    {
        super.wasAdded();
        // our handle can be changed, so set it every time
        PlayerObject user = _ctx.getUserObject();
        _handle.setText(user.handle.toString());
    }

    @Override // documentation inherited
    protected void gotFocus ()
    {
        super.gotFocus();
        _posterBtn.setEnabled(true);
    }

    protected void selectTab (int tabidx)
    {
        // create our tabs on the fly
        BComponent tab;
        switch (_selectedTab = tabidx) {
        default:
        case 0:
            if (_items == null) {
                _items = new InventoryPalette(_ctx, INV_PRED, true);
            }
            tab = _items;
            break;

        case 1:
            if (_bigshots == null) {
                _bigshots = new UnitPalette(_ctx, null, 5, 3);
                _bigshots.setSelectable(0);
                _bigshots.setUser(_ctx.getUserObject(), false);
            }
            tab = _bigshots;
            break;

        case 2:
            if (_badges == null) {
                _badges = new BadgePalette(_ctx);
            }
            tab = _badges;
            break;

        case 3:
            if (_duds == null) {
                _duds = new InventoryPalette(_ctx, DUDS_PRED, true);
            }
            tab = _duds;
            break;

        case 4:
            if (_pardners == null) {
                _pardners = new PardnerView(_ctx);
            }
            tab = _pardners;
            break;
        }

        if (tab != _tab) {
            if (_tab != null) {
                remove(_tab);
            }
            add(_tab = tab, TVIEW_BOUNDS);
            _tabinfo.setText(_msgs.get("m.status_" + TABS[tabidx] + "_info"));
        }
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;

    protected HackyTabs _tabs;
    protected BComponent _tab;
    protected BLabel _tabinfo, _handle;
    protected BButton _posterBtn;

    protected InventoryPalette _items, _badges, _duds;
    protected UnitPalette _bigshots;
    protected PardnerView _pardners;

    /** We track which tab was last selected across instances. */
    protected static int _selectedTab;

    protected static final String[] TABS = {
        "items", "bigshots", "badges", "duds", "pardners" };
    protected static final Rectangle TVIEW_BOUNDS =
        new Rectangle(287, 70, PaletteIcon.ICON_SIZE.width * 5,
                      PaletteIcon.ICON_SIZE.height * 3 + 37);

    protected static final Predicate<Item> INV_PRED = new Predicate<Item>() {
        public boolean isMatch (Item item) {
            return !(item instanceof Badge) && !(item instanceof Article) &&
                !(item instanceof BigShotItem);
        }
    };

    protected static final Predicate<Item> DUDS_PRED =
        new Predicate.InstanceOf<Item>(Article.class);

    // TODO: sort out how we'll localize these
    protected static final int[] STATUS_KEYMAP = {
        Keys.I, ITEMS_TAB,
        Keys.S, BIGSHOTS_TAB,
        Keys.B, BADGES_TAB,
        Keys.D, DUDS_TAB,
        Keys.P, PARDNERS_TAB,
    };
}
