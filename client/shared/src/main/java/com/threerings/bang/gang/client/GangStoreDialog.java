//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BLabel;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Point;

import com.threerings.presents.dobj.DObject;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.util.BangContext;

import com.threerings.bang.avatar.client.BuckleView;

import com.threerings.bang.store.client.GoodsInspector;
import com.threerings.bang.store.client.GoodsPalette;
import com.threerings.bang.store.data.Good;

import com.threerings.bang.gang.data.GangGood;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;
import com.threerings.bang.gang.data.WeightClassUpgradeGood;

/**
 * Allows gang leaders to purchase buckle parts and upgrades for their gangs.
 */
public class GangStoreDialog extends BDecoratedWindow
    implements ActionListener, HideoutCodes
{
    public GangStoreDialog (BangContext ctx, HideoutObject hideoutobj, GangObject gangobj)
    {
        super(ctx.getStyleSheet(), ctx.xlate(HIDEOUT_MSGS, "t.gang_store_dialog"));
        setStyleClass("gang_store_dialog");
        setModal(true);
        _ctx = ctx;
        _hideoutobj = hideoutobj;
        _gangobj = gangobj;
        _msgs = ctx.getMessageManager().getBundle(HIDEOUT_MSGS);

        ((GroupLayout)getLayoutManager()).setGap(0);

        BContainer pcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        pcont.setStyleClass("gang_store_goods");
        ((GroupLayout)pcont.getLayoutManager()).setOffAxisJustification(GroupLayout.TOP);
        ((GroupLayout)pcont.getLayoutManager()).setGap(0);
        add(pcont, GroupLayout.FIXED);

        BContainer lcont = new BContainer(new BorderLayout(-5, 0));
        pcont.add(lcont);

        ImageIcon divicon = new ImageIcon(_ctx.loadImage("ui/hideout/vertical_divider.png"));
        lcont.add(new BLabel(divicon), BorderLayout.EAST);

        BContainer ltcont = GroupLayout.makeVBox(GroupLayout.TOP);
        ((GroupLayout)ltcont.getLayoutManager()).setOffAxisJustification(GroupLayout.RIGHT);
        ((GroupLayout)ltcont.getLayoutManager()).setGap(8);
        lcont.add(ltcont, BorderLayout.CENTER);

        BContainer acont = GroupLayout.makeVBox(GroupLayout.TOP);
        acont.setStyleClass("gang_store_icon_left");

        acont.add(new BLabel(new ImageIcon(_ctx.loadImage("ui/hideout/gang_gear_symbol.png"))));
        acont.add(new BLabel(_msgs.get("m.gang_gear"), "gang_store_scroll"));
        ltcont.add(acont);

        ltcont.add(_ltabs =
            new HackyTabs(ctx, true, "ui/hideout/store_tab_", LEFT_TABS, 85, 10) {
                protected void tabSelected (int index) {
                    GangStoreDialog.this.tabSelected(index, false);
                }
            });
        _ltabs.setStyleClass("gang_store_tabs_left");
        _ltabs.setDefaultTab(-1);

        pcont.add(_palette = new GoodsPalette(_ctx, 5, 3) {
            protected boolean isAvailable (Good good) {
                return ((GangGood)good).isAvailable(_gangobj);
            }
            protected DObject getColorEntity () {
                return _gangobj;
            }
        });
        _palette.init(hideoutobj);
        _palette.setPaintBackground(true);
        _palette.setShowNavigation(false);

        BContainer rcont = new BContainer(new BorderLayout(-5, 0));
        pcont.add(rcont);

        rcont.add(new BLabel(divicon), BorderLayout.WEST);

        BContainer rtcont = GroupLayout.makeVBox(GroupLayout.TOP);
        ((GroupLayout)rtcont.getLayoutManager()).setOffAxisJustification(GroupLayout.LEFT);
        ((GroupLayout)rtcont.getLayoutManager()).setGap(11);
        rcont.add(rtcont, BorderLayout.CENTER);

        acont = GroupLayout.makeVBox(GroupLayout.TOP);
        ((GroupLayout)acont.getLayoutManager()).setGap(9);
        acont.setStyleClass("gang_store_icon_right");
        BuckleView bview = new BuckleView(ctx, 3);
        bview.setBuckle(gangobj.getBuckleInfo());
        acont.add(bview);
        acont.add(new BLabel(_msgs.get("m.buckles"), "gang_store_scroll"));
        rtcont.add(acont);

        rtcont.add(_rtabs =
            new HackyTabs(ctx, true, "ui/hideout/store_tab_", RIGHT_TABS, 85, 10) {
                protected void tabSelected (int index) {
                    GangStoreDialog.this.tabSelected(index, true);
                }
            });
        _rtabs.setStyleClass("gang_store_tabs_right");
        _rtabs.setDefaultTab(-1);

        add(new Spacer(1, -4), GroupLayout.FIXED);
        BContainer ncont = GroupLayout.makeHBox(GroupLayout.RIGHT);
        ncont.setPreferredSize(new Dimension(715, -1));
        ncont.add(_palette.getNavigationContainer());
        add(ncont, GroupLayout.FIXED);

        add(new Spacer(1, -10), GroupLayout.FIXED);
        BContainer ccont = new BContainer(GroupLayout.makeHStretch());
        ccont.setPreferredSize(900, 160);
        add(ccont, GroupLayout.FIXED);

        ccont.add(new Spacer(30, 1), GroupLayout.FIXED);
        ccont.add(_inspector = new GangGoodsInspector(_ctx, _palette));
        _inspector.init(hideoutobj);
        _palette.setInspector(_inspector);

        BContainer dcont = GroupLayout.makeVBox(GroupLayout.CENTER);
        dcont.setStyleClass("gang_store_controls");
        ((GroupLayout)dcont.getLayoutManager()).setPolicy(GroupLayout.STRETCH);
        BContainer cofcont = GroupLayout.makeVBox(GroupLayout.CENTER);
        cofcont.add(new BLabel(_msgs.get("m.coffers"), "coffer_label"));
        cofcont.add(new CofferLabel(ctx, gangobj));
        dcont.add(cofcont);
        dcont.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"), GroupLayout.FIXED);
        ccont.add(dcont, GroupLayout.FIXED);

        add(_status = new StatusLabel(ctx), GroupLayout.FIXED);
        _status.setStatus(" ", false); // make sure it takes up space
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        if (event.getAction().equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);
        }
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _ltabs.selectTab(0);
    }

    protected void tabSelected (int index, boolean right)
    {
        if (index != -1) {
            (right ? _ltabs : _rtabs).selectTab(-1, false);
            final String prefix = (right ? RIGHT_TABS : LEFT_TABS)[index];
            _palette.setFilter(new GoodsPalette.Filter() {
                public boolean isValid (Good good) {
                    return good.getType().startsWith(prefix);
                }
            });
        }
    }

    /**
     * Shows a warning about the perils of downgrading
     */
    protected void showDowngradeWarning ()
    {
        OptionDialog.showConfirmDialog(
                _ctx, HIDEOUT_MSGS, "m.downgrade_warning", new String[] { "m.ok" }, null);
    }

    protected class GangGoodsInspector extends GoodsInspector
    {
        public GangGoodsInspector (BangContext ctx, GoodsPalette palette)
        {
            super(ctx, palette);
            _icon.setStyleClass("gang_store_good");
            _quote = new BButton(_msgs.get("m.quote"), this, "quote");
            _quote.setStyleClass("big_button");
        }

        public void iconUpdated (SelectableIcon icon, boolean selected)
        {
            super.iconUpdated(icon, selected);

            if (_good instanceof WeightClassUpgradeGood) {
                remove(_buy);
                if (!_quote.isAdded()) {
                    add(_quote, new Point(500 + getControlGapOffset(), 10));
                }
                _mode = Mode.NEW;
            } else if (_quote.isAdded()) {
                remove(_quote);
            }
        }

        public void actionPerformed (ActionEvent event)
        {
            super.actionPerformed(event);

            if (_good == null) {
                return;
            }
            final GangGood fgood = (GangGood)_good;

            if ("quote".equals(event.getAction())) {
                HideoutService.ResultListener rl = new HideoutService.ResultListener() {
                    public void requestProcessed (Object result) {
                        if (_good != fgood) {
                            return;
                        }
                        int[] costs = (int[])result;
                        ((GangMoneyLabel)_cost).setMoney(
                            fgood.getScripCost(_gangobj) + costs[1],
                            fgood.getCoinCost(_gangobj) + costs[0],
                            fgood.getAceCost(_gangobj), false);
                        remove(_quote);
                        add(_buy, new Point(500 + getControlGapOffset(), 10));
                        if (fgood instanceof WeightClassUpgradeGood &&
                                ((WeightClassUpgradeGood)fgood).getWeightClass() <
                                _gangobj.getWeightClass()) {
                            showDowngradeWarning();
                        }
                    }
                    public void requestFailed (String cause) {
                        _quote.setEnabled(true);
                        _status.setStatus(_msgs.xlate(cause), true);
                    }
                };
                _hideoutobj.service.getUpgradeQuote(fgood, rl);
            }
        }

        protected String purchasedKey ()
        {
            return "m.gang_purchased";
        }

        protected int getControlGapOffset ()
        {
            return 10;
        }

        protected MoneyLabel createCostLabel ()
        {
            return new GangMoneyLabel(_ctx, false);
        }

        protected void updateCostLabel ()
        {
            GangGood good = (GangGood)_good;
            ((GangMoneyLabel)_cost).setMoney(good.getScripCost(_gangobj),
                good.getCoinCost(_gangobj), good.getAceCost(_gangobj), false);
        }

        protected BButton _quote;
    }

    protected BangContext _ctx;
    protected HideoutObject _hideoutobj;
    protected GangObject _gangobj;
    protected MessageBundle _msgs;

    protected HackyTabs _ltabs, _rtabs;
    protected GoodsPalette _palette;
    protected GoodsInspector _inspector;
    protected StatusLabel _status;

    protected static final String[] LEFT_TABS = { "upgrade", "unlock" };
    protected static final String[] RIGHT_TABS = { "icon", "border", "background" };
}
