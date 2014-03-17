//
// $Id$

package com.threerings.bang.gang.client;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.jmex.bui.util.Dimension;

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

import com.samskivert.util.Predicate;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.ItemIcon;
import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.client.bui.StatusLabel;

import com.threerings.bang.data.Article;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.BuckleUpgrade;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.WeightClassUpgrade;

import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;

import com.threerings.bang.avatar.client.BuckleView;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;
import com.threerings.bang.gang.data.RentalGood;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

/**
 * Displays gang owned items and allows renewal of rented items.
 */
public class GangInventory extends BDecoratedWindow
    implements ActionListener, IconPalette.Inspector, HideoutCodes, GangCodes
{
    public GangInventory (BangContext ctx, HideoutObject hideoutobj, GangObject gangobj)
    {
        super(ctx.getStyleSheet(), ctx.xlate(HIDEOUT_MSGS, "t.gang_inventory"));
        setStyleClass("outfit_dialog");
        setModal(true);
        _ctx = ctx;
        _hideoutobj = hideoutobj;
        _gangobj = gangobj;
        _msgs = ctx.getMessageManager().getBundle(HIDEOUT_MSGS);

        ((GroupLayout)getLayoutManager()).setGap(0);

        BContainer pcont = GroupLayout.makeHBox(GroupLayout.CENTER);
        pcont.setStyleClass("outfit_articles");
        ((GroupLayout)pcont.getLayoutManager()).setOffAxisJustification(GroupLayout.TOP);
        ((GroupLayout)pcont.getLayoutManager()).setGap(0);
        add(pcont, GroupLayout.FIXED);

        BContainer lcont = new BContainer(new BorderLayout(-5, 0));
        pcont.add(lcont);

        ImageIcon divicon = new ImageIcon(_ctx.loadImage("ui/hideout/vertical_divider.png"));
        lcont.add(new BLabel(divicon), BorderLayout.EAST);

        BContainer ltcont = GroupLayout.makeVBox(GroupLayout.TOP);
        ((GroupLayout)ltcont.getLayoutManager()).setOffAxisJustification(GroupLayout.RIGHT);
        lcont.add(ltcont, BorderLayout.CENTER);

        BContainer acont = GroupLayout.makeVBox(GroupLayout.TOP);
        acont.setStyleClass("gang_store_icon_left");
        BuckleView bview = new BuckleView(ctx, 3);
        bview.setBuckle(gangobj.getBuckleInfo());
        acont.add(bview);
        acont.add(new BLabel(_msgs.get("m.gang_inventory"), "gang_store_scroll"));
        ltcont.add(acont);

        ltcont.add(_ltabs =
            new HackyTabs(ctx, true, "ui/hideout/inventory_tab_", TABS, 85, 10) {
                protected void tabSelected (int index) {
                    GangInventory.this.tabSelected(index);
                }
            });
        _ltabs.setStyleClass("gang_inventory_tabs");
        _ltabs.setDefaultTab(-1);

        pcont.add(_palette = new GangInventoryPalette(this));

        add(new Spacer(1, -4), GroupLayout.FIXED);
        BContainer ncont = GroupLayout.makeHBox(GroupLayout.RIGHT);
        ncont.setPreferredSize(new Dimension(715, -1));
        ncont.add(_palette.getNavigationContainer());
        add(ncont, GroupLayout.FIXED);

        add(new Spacer(1, -10), GroupLayout.FIXED);
        BContainer ccont = new BContainer(GroupLayout.makeHStretch());
        ccont.setPreferredSize(900, 160);
        add(ccont, GroupLayout.FIXED);

        ccont.add(_icont = GroupLayout.makeHBox(GroupLayout.LEFT));
        ((GroupLayout)_icont.getLayoutManager()).setGap(10);
        _palette.setInspector(this);

        if (DeploymentConfig.usesCoins() && _ctx.getUserObject().gangRank == LEADER_RANK) {
            BContainer bcont = GroupLayout.makeVBox(GroupLayout.CENTER);
            bcont.setStyleClass("outfit_controls");
            ((GroupLayout)bcont.getLayoutManager()).setPolicy(GroupLayout.STRETCH);
            bcont.add(new BLabel(_msgs.get("m.renew_until"), "outfit_total_price"),
                    GroupLayout.FIXED);
            bcont.add(_dlabel = new BLabel("", "goods_descrip"));
            bcont.add(_renew = new BButton(_msgs.get("m.renew_item"), this, "renew_item"),
                GroupLayout.FIXED);
            _renew.setEnabled(false);
            ccont.add(bcont, GroupLayout.FIXED);
        }

        BContainer dcont = GroupLayout.makeVBox(GroupLayout.CENTER);
        dcont.setStyleClass("outfit_controls");
        ((GroupLayout)dcont.getLayoutManager()).setPolicy(GroupLayout.STRETCH);
        BContainer cofcont = GroupLayout.makeVBox(GroupLayout.CENTER);
        cofcont.add(new BLabel(_msgs.get("m.coffers"), "coffer_label"));
        cofcont.add(_coffers = new CofferLabel(ctx, gangobj));
        dcont.add(cofcont);
        dcont.add(new BButton(_msgs.get("m.dismiss"), this, "dismiss"), GroupLayout.FIXED);
        ccont.add(dcont, GroupLayout.FIXED);

        add(_status = new StatusLabel(ctx), GroupLayout.FIXED);
        _status.setStatus(" ", false); // make sure it takes up space
    }

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("renew_item")) {
            if (_item == null || _renew == null) {
                return;
            }
            _renew.setEnabled(false);
            HideoutService.ConfirmListener cl = new HideoutService.ConfirmListener() {
                public void requestProcessed () {
                }
                public void requestFailed (String cause) {
                    _renew.setEnabled(true);
                    _status.setStatus(_msgs.xlate(cause), true);
                }
            };
            _hideoutobj.service.renewGangItem(_item.getItemId(), cl);

        } else if (action.equals("dismiss")) {
            _ctx.getBangClient().clearPopup(this, true);
        }
    }

    // documentation inherited from interface IconPalette.Inspector
    public void iconUpdated (SelectableIcon icon, boolean selected)
    {
        if (_renew != null) {
            _renew.setEnabled(selected);
        }
        _icont.removeAll();
        if (!selected) {
            _item = null;
            return;
        }
        ItemIcon iicon = (ItemIcon)icon;
        _item = iicon.getItem();
        _icont.add(new BLabel(icon.getIcon(), "outfit_item"));

        BContainer dcont = new BContainer(GroupLayout.makeVStretch());
        dcont.setPreferredSize(-1, 135);
        dcont.add(new BLabel(_msgs.xlate(_item.getName()), "medium_title"), GroupLayout.FIXED);
        String msg;
        if (_item.getExpires() != 0) {
            msg = MessageBundle.compose("m.renew_tip",
                    MessageBundle.taint(Item.EXPIRE_FORMAT.format(new Date(_item.getExpires()))));
            if (_dlabel != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(_item.getExpires());
                cal.add(Calendar.DAY_OF_YEAR, 30);
                _dlabel.setText("@=b(" + Item.EXPIRE_FORMAT.format(cal.getTime()) + ")");
            }
        } else {
            msg = "m.item_tip";
        }
        BLabel tlabel = new BLabel(_msgs.xlate(msg), "goods_descrip");
        tlabel.setPreferredSize(350, -1);
        dcont.add(tlabel);
        BContainer pcont = GroupLayout.makeHBox(GroupLayout.LEFT);
        pcont.add(new BLabel(_msgs.get("m.renew_price"), "table_data"));
        RentalGood good = _hideoutobj.getRentalGood(_item);
        if (good != null) {
            MoneyLabel plabel = new MoneyLabel(_ctx);
            plabel.setMoney(
                    good.getRentalScripCost(_gangobj), good.getRentalCoinCost(_gangobj), false);
            pcont.add(plabel);
        }
        dcont.add(pcont, GroupLayout.FIXED);
        _icont.add(dcont);
    }

    @Override // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();
        _ltabs.selectTab(0);
        _gangobj.addListener(_palette);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        _gangobj.removeListener(_palette);
    }

    /**
     * Called when the user selects an inventory tab.
     */
    protected void tabSelected (int index)
    {
        switch(index) {
        case 0:
            _palette.setPredicate(ITEM_PRED);
            break;
        case 1:
            _palette.setPredicate(COWBOY_PRED);
            break;
        case 2:
            _palette.setPredicate(COWGIRL_PRED);
            break;
        }
    }

    protected class GangInventoryPalette extends IconPalette
        implements SetListener<Item>
    {
        public GangInventoryPalette (Inspector inspector)
        {
            super(inspector, COLUMNS, ROWS, ItemIcon.ICON_SIZE, 1);
            setPaintBackground(true);
            setShowNavigation(false);
        }

        // documentation inherited from SetListener
        public void entryAdded (EntryAddedEvent<Item> event)
        {
            if (event.getName().equals(GangObject.INVENTORY)) {
                Item item = event.getEntry();
                if (_itemp.isMatch(item)) {
                    int idx = 0;
                    for (SelectableIcon icon : _icons) {
                        ItemIcon iicon = (ItemIcon)icon;
                        if (_itemComparator.compare(item, iicon.getItem()) < 0) {
                            break;
                        } else {
                            idx++;
                        }
                    }
                    addIcon(idx, new ItemIcon(_ctx, item));
                }
            }

        }

        // documentation inherited from SetListener
        public void entryRemoved (EntryRemovedEvent<Item> event)
        {
            if (event.getName().equals(GangObject.INVENTORY)) {
                Item item = event.getOldEntry();
                if (_itemp.isMatch(item)) {
                    removeIcon(getIcon(item));
                }
            }
        }

        // documentation inherited from SetListener
        public void entryUpdated (EntryUpdatedEvent<Item> event)
        {
            if (event.getName().equals(GangObject.INVENTORY)) {
                Item item = event.getEntry();
                if (_itemp.isMatch(item)) {
                    ItemIcon iicon = getIcon(item);
                    iicon.setItem(item);
                    if (iicon.isSelected() && _inspector != null) {
                        _inspector.iconUpdated(iicon, true);
                    }
                }
            }
        }

        /**
         * Sets the predicate which will determine the items to show on the palette.
         */
        public void setPredicate (Predicate<Item> itemp)
        {
            clear();
            List<Item> items = _gangobj.inventorySnapshot();
            Collections.sort(items, _itemComparator);
            _itemp = itemp;
            for (Item item : items) {
                if (!_itemp.isMatch(item)) {
                    continue;
                }
                ItemIcon icon = new ItemIcon(_ctx, item);
                icon.setMenuEnabled(false);
                addIcon(icon);
            }
        }

        /**
         * Finds the icon corresponding to the specified item.
         */
        protected ItemIcon getIcon (Item item)
        {
            int itemId = item.getItemId();
            for (SelectableIcon icon : _icons) {
                ItemIcon iicon = (ItemIcon)icon;
                if (iicon.getItem().getItemId() == itemId) {
                    return iicon;
                }
            }
            return null;
        }

        protected Predicate<Item> _itemp;

        /** Used to sort the inventory display. */
        protected Comparator<Item> _itemComparator = new Comparator<Item>() {
            public int compare (Item one, Item two) {
                if (one.getExpires() != 0 && two.getExpires() != 0) {
                    return Long.signum(two.getExpires() - one.getExpires());
                } else if (one.getExpires() != 0) {
                    return -1;
                } else if (two.getExpires() != 0) {
                    return 1;
                } else {
                    String t1 = _ctx.xlate(BangCodes.BANG_MSGS, one.getName(false));
                    return t1.compareTo(_ctx.xlate(BangCodes.BANG_MSGS, two.getName(false)));
                }
            }
        };

        protected static final int COLUMNS = 6;
        protected static final int ROWS = 3;
    }

    protected BangContext _ctx;
    protected MessageBundle _msgs;
    protected HideoutObject _hideoutobj;
    protected GangObject _gangobj;

    protected HackyTabs _ltabs;
    protected BLabel _dlabel;
    protected CofferLabel _coffers;
    protected BButton _renew;
    protected GangInventoryPalette _palette;
    protected BContainer _icont;
    protected StatusLabel _status;
    protected Item _item;

    protected static final String[] TABS = { "items", "cowboy", "cowgirl" };

    protected static final Predicate<Item> ITEM_PRED = new Predicate<Item>() {
        public boolean isMatch (Item item) {
            return !(item instanceof Article) && !(item instanceof BucklePart) &&
                        !(item instanceof BuckleUpgrade) && !(item instanceof WeightClassUpgrade);
        }
    };
    protected static final Predicate<Item> COWBOY_PRED = new Predicate<Item>() {
        public boolean isMatch (Item item) {
            return item instanceof Article && ((Article)item).getName().indexOf("female") == -1;
        }
    };
    protected static final Predicate<Item> COWGIRL_PRED = new Predicate<Item>() {
        public boolean isMatch (Item item) {
            return item instanceof Article && ((Article)item).getName().indexOf("female") != -1;
        }
    };
}
