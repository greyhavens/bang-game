//
// $Id$

package com.threerings.bang.client;

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

import com.threerings.util.MessageBundle;

import com.threerings.bang.avatar.client.PickLookView;
import com.threerings.bang.ranch.client.UnitPalette;

import com.threerings.bang.data.Article;
import com.threerings.bang.data.Badge;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.Stat;

import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.client.bui.PaletteIcon;
import com.threerings.bang.client.util.EscapeListener;
import com.threerings.bang.util.BangContext;

/**
 * Displays a summary of a player's status, including: cash on hand,
 * inventory items, big shots.
 */
public class StatusView extends BWindow
    implements ActionListener
{
    public StatusView (BangContext ctx)
    {
        super(ctx.getStyleSheet(), new AbsoluteLayout());
        setStyleClass("status_view");

        _ctx = ctx;
        _msgs = ctx.getMessageManager().getBundle(BangCodes.BANG_MSGS);
        setModal(true);
        addListener(new EscapeListener() {
            public void escapePressed() {
                _ctx.getBangClient().clearPopup();
            }
        });

        PlayerObject user = ctx.getUserObject();
        add(new BLabel(user.handle.toString(), "status_handle"),
            new Rectangle(40, 590, 195, 33));
        BButton btn = new BButton(_msgs.get("m.status_poster"), this, "poster");
        btn.setStyleClass("big_button");
        btn.setEnabled(false); // TODO
        add(btn, new Point(40, 147));

        add(new PickLookView(ctx), new Point(10, 231));
        add(new WalletLabel(ctx, true), new Rectangle(77, 63, 150, 40));
        add(_tabinfo = new BLabel("", "status_tabinfo"),
            new Rectangle(290, 55, 453, 40));

        BContainer btns = GroupLayout.makeHBox(GroupLayout.CENTER);
        ((GroupLayout)btns.getLayoutManager()).setGap(15);
        btns.add(new BButton(_msgs.get("m.status_quit"), this, "quit"));
        btns.add(new BButton(_msgs.get("m.status_to_town"), this, "to_town"));
        btns.add(new BButton(_msgs.get("m.status_resume"), this, "resume"));
        add(btns, new Rectangle(652, 8, 310, 35));

        add(new HackyTabs(ctx, false, "ui/status/tab_", TABS, 137, 17) {
            protected void tabSelected (int index) {
                StatusView.this.selectTab(index);
            }
        }, new Rectangle(265, 575, 15+5*140, 66));

        // start with the inventory tab selected
        selectTab(_selectedTab);
    }

    /**
     * Binds this menu to a component. The component should either be the
     * default event target, or a modal window.
     */
    public void bind (BComponent host)
    {
        host.addListener(new EscapeListener() {
            public void escapePressed () {
                _ctx.getBangClient().displayPopup(StatusView.this);
                pack();
                center();
            }
        });
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String cmd = event.getAction();
        if (cmd.equals("quit")) {
            _ctx.getApp().stop();

        } else if (cmd.equals("to_town")) {
            if (_ctx.getLocationDirector().leavePlace()) {
                _ctx.getBangClient().clearPopup();
                _ctx.getBangClient().showTownView();
            }

        } else if (cmd.equals("resume")) {
            _ctx.getBangClient().clearPopup();
        }
    }

    protected void selectTab (int tabidx)
    {
        // create our tabs on the fly
        BComponent tab;
        switch (_selectedTab = tabidx) {
        default:
        case 0:
            if (_items == null) {
                _items = new InventoryPalette(_ctx, INV_PRED);
            }
            tab = _items;
            break;

        case 1:
            if (_bigshots == null) {
                _bigshots = new UnitPalette(_ctx, null, 4, 3);
                _bigshots.setUser(_ctx.getUserObject());
            }
            tab = _bigshots;
            break;

        case 2:
            if (_badges == null) {
                _badges = new InventoryPalette(_ctx, BADGE_PRED);
            }
            tab = _badges;
            break;

        case 3:
            if (_duds == null) {
                _duds = new InventoryPalette(_ctx, DUDS_PRED);
            }
            tab = _duds;
            break;

        case 4:
            if (_stats == null) {
                _stats = new PlayerStatsView(_ctx);
            }
            tab = _stats;
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

    protected BComponent _tab;
    protected BLabel _tabinfo;

    protected InventoryPalette _items, _badges, _duds;
    protected UnitPalette _bigshots;
    protected PlayerStatsView _stats;

    /** We track which tab was last selected across instances. */
    protected static int _selectedTab;

    protected static final String[] TABS = {
        "items", "bigshots", "badges", "duds", "stats" };
    protected static final Rectangle TVIEW_BOUNDS =
        new Rectangle(287, 70, PaletteIcon.ICON_SIZE.width * 5,
                      PaletteIcon.ICON_SIZE.height * 3 + 37);

    protected static final InventoryPalette.Predicate INV_PRED =
        new InventoryPalette.Predicate() {
        public boolean includeItem (Item item) {
            return !(item instanceof Badge) && !(item instanceof Article);
        }
    };

    protected static final InventoryPalette.Predicate BADGE_PRED =
        new InventoryPalette.Predicate() {
        public boolean includeItem (Item item) {
            return (item instanceof Badge);
        }
    };

    protected static final InventoryPalette.Predicate DUDS_PRED =
        new InventoryPalette.Predicate() {
        public boolean includeItem (Item item) {
            return (item instanceof Article);
        }
    };
}
