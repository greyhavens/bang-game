//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BComboBox;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BTabbedPane;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.TextEvent;
import com.jmex.bui.event.TextListener;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.text.IntegerDocument;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.samskivert.util.QuickSort;
import com.samskivert.util.RandomUtil;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.util.MessageBundle;

import com.threerings.bang.client.BangUI;
import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.client.bui.OptionDialog;
import com.threerings.bang.client.bui.PaletteIcon;
import com.threerings.bang.client.bui.RequestButton;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.NameFactory;
import com.threerings.bang.util.NameValidator;

import com.threerings.bang.saloon.client.TopScoreView;

import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.data.HideoutCodes;
import com.threerings.bang.gang.data.HideoutObject;

import static com.threerings.bang.Log.log;

/**
 * Displays the hideout interface, where players can form or manage gangs,
 * see what other gang members are up to, view rankings within the gang,
 * etc.
 */
public class HideoutView extends ShopView
{
    public HideoutView (BangContext ctx)
    {
        super(ctx, HideoutCodes.HIDEOUT_MSGS);

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

        // create our tabs
        _gtab = new BContainer(new AbsoluteLayout());
        add(_tabs = new HackyTabs(
            ctx, false, "ui/hideout/tab_", TABS, 136, 17) {
            public void selectTab (int index, boolean feedback) {
                // only the first tab is selectable for users not in gangs
                // or waiting for the gang object to load
                if (_ctx.getUserObject().gangId <= 0 || _gangobj == null) {
                    index = 0;
                }
                super.selectTab(index, feedback);
            }
            protected void tabSelected (int index) {
                HideoutView.this.selectTab(index);
            }
        }, TABS_RECT);
        
        // start with a random shop tip
        _status.setStatus(getShopTip(), false);
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        _hideoutobj = (HideoutObject)plobj;
        
        // populate the gang tab
        updateGangTab();
        
        // listen for changes in gang membership
        _ctx.getUserObject().addListener(_gtupdater);
    }
    
    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
        // stop listening to the user
        _ctx.getUserObject().removeListener(_gtupdater);
        
        // unsubscribe from the gang object
        unsubscribeFromGang();
    }
    
    /**
     * Called when a tab is selected.
     */
    protected void selectTab (int tidx)
    {
        BContainer tab;
        if (tidx == 0) {
            tab = _gtab;
        } else if (tidx == 1) {
            if (_mtab == null) {
                _mtab = new MemberView(_ctx, _status, _hideoutobj, _gangobj);
            }
            tab = _mtab;
        } else if (tidx == 2) {
            if (_rtab == null) {
                _rtab = new TopScoreView(_ctx, _gangobj);
            }
            tab = _rtab;
        } else { // tidx == 3
            if (_htab == null) {
                _htab = new HistoryView(_ctx, _status, _hideoutobj);
            }
            tab = _htab;
        }
        if (_stab != tab) {
            if (_stab != null) {
                remove(_stab);
            }
            add(_stab = tab, TAB_RECT);
        }
    }
    
    /**
     * Sets the contents of the gang tab based on whether or not the user is
     * in a gang.
     */
    protected void updateGangTab ()
    {
        _gtab.removeAll();
        
        // if the user is not in a gang, make sure that we are not subscribed
        // to one and show the form gang panel
        PlayerObject player = _ctx.getUserObject();
        if (player.gangOid <= 0) {
            unsubscribeFromGang();
            _gtab.add(createFormGangPanel(), new Point(100, 100));
            _mtab = null;
            return;
        }
        
        // we first need the gang oid (which may take time to set if the gang
        // must be loaded from the database) and then the gang object itself
        _gtab.add(new BLabel(_msgs.get("m.loading_gang")),
            new Point(100, 100));
        (_gangsub = new SafeSubscriber<GangObject>(
            player.gangOid, new Subscriber<GangObject>() {
            public void objectAvailable (GangObject gangobj) {
                _gangobj = gangobj;
                _gangobj.addListener(_ganglist);
                continueUpdatingGangTab();        
            }
            public void requestFailed (int oid, ObjectAccessException cause) {
                log.warning("Failed to subscribe to gang object [oid=" + oid +
                    ", cause=" + cause + "].");
                _status.setStatus(_msgs.get("m.internal_error"), true);
            }
        })).subscribe(_ctx.getDObjectManager());
    }
    
    /**
     * Continues the process of updating the gang tab once the gang object is
     * available.
     */
    protected void continueUpdatingGangTab ()
    {
        _gtab.removeAll();
        _gtab.add(createLeaveGangButton(), new Point(100, 100));
        _gtab.add(createDonatePanel(), new Point(200, 200));
    }
    
    /**
     * Creates and returns an instance of the panel used to form a new gang.
     */
    protected BContainer createFormGangPanel ()
    {
        BContainer fcont = new BContainer(GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.CENTER, GroupLayout.EQUALIZE));
        fcont.add(new BLabel(_msgs.get("m.form_gang_tip")));
        
        BContainer ncont = new BContainer(GroupLayout.makeHStretch());
        fcont.add(ncont);
        ncont.add(new BLabel(_msgs.get("m.gang_name")), GroupLayout.FIXED);
        ncont.add(new BLabel(_ctx.xlate(GangCodes.GANG_MSGS, "m.gang_prefix")),
            GroupLayout.FIXED);
        
        // initialize the root with the player's name
        final BTextField root = new BTextField(
            _ctx.getUserObject().handle.toString(),
            NameFactory.getValidator().getMaxHandleLength());
        ncont.add(root);
        
        // sort the gang name suffixes by name and start with a random one
        String[] suffixes =
            NameFactory.getCreator().getGangSuffixes().toArray(new String[0]);
        QuickSort.sort(suffixes);
        final BComboBox suffix = new BComboBox(suffixes);
        suffix.selectItem(RandomUtil.getInt(suffixes.length));
        ncont.add(suffix, GroupLayout.FIXED);
        
        BContainer ccont = GroupLayout.makeHBox(GroupLayout.RIGHT);
        fcont.add(ccont);
        ccont.add(new BLabel(_msgs.get("m.form_cost")));
        MoneyLabel mlabel = new MoneyLabel(_ctx);
        mlabel.setMoney(GangCodes.FORM_GANG_SCRIP_COST,
            GangCodes.FORM_GANG_COIN_COST, false);
        ccont.add(mlabel);
        RequestButton fbutton = new RequestButton(
            _ctx, HideoutCodes.HIDEOUT_MSGS, "m.form_gang", _status) {
            protected void fireRequest () {
                _hideoutobj.service.formGang(_ctx.getClient(),
                    new Handle(root.getText()),
                    (String)suffix.getSelectedItem(), this);
            }
        };
        new EnablingValidator(root, fbutton) {
            protected boolean checkEnabled (String text) {
                return NameFactory.getValidator().isValidHandle(
                    new Handle(text));
            }
        };
        ccont.add(fbutton);
        
        return fcont;
    }
    
    /**
     * Creates and returns an instance of the button used to leave the gang.
     */
    protected BButton createLeaveGangButton ()
    {
        String msg = MessageBundle.tcompose("m.confirm_leave", _gangobj.name);
        RequestButton lbutton = new RequestButton(
            _ctx, HideoutCodes.HIDEOUT_MSGS, "m.leave_gang", msg, _status) {
            protected void fireRequest () {
                _hideoutobj.service.leaveGang(_ctx.getClient(), this);
            }
        };
        return lbutton;
    }
    
    /**
     * Creates and returns an instance of the panel used to donate to the
     * gang's coffers.
     */
    protected BContainer createDonatePanel ()
    {
        BContainer dcont = new BContainer(GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.CENTER, GroupLayout.EQUALIZE));
        dcont.add(new BLabel(_msgs.get("m.donate_tip")));
        
        BContainer mcont = GroupLayout.makeHBox(GroupLayout.RIGHT);
        dcont.add(mcont);
        mcont.add(new BLabel(_msgs.get("m.in_coffers")));
        mcont.add(_coffers = new MoneyLabel(_ctx));
        _coffers.setMoney(_gangobj.scrip, _gangobj.coins, false);
        
        BContainer bcont = GroupLayout.makeHBox(GroupLayout.RIGHT);
        dcont.add(bcont);
        bcont.add(new BLabel(_msgs.get("m.donate")));
        bcont.add(new BLabel(BangUI.scripIcon));
        final BTextField scrip = new BTextField(4);
        scrip.setDocument(new IntegerDocument(true));
        bcont.add(scrip);
        bcont.add(new BLabel(_msgs.get("m.and")));
        bcont.add(new BLabel(BangUI.coinIcon));
        final BTextField coins = new BTextField(4);
        coins.setDocument(new IntegerDocument(true));
        bcont.add(coins);
        final RequestButton dbutton = new RequestButton(
            _ctx, HideoutCodes.HIDEOUT_MSGS, "m.donate", _status) {
            public void requestProcessed () {
                super.requestProcessed();
                scrip.setText("");
                coins.setText("");
            }
            protected void fireRequest () {
                _hideoutobj.service.addToCoffers(
                    _ctx.getClient(),
                    parseInt(scrip.getText()),
                    parseInt(coins.getText()), this);
            }
        };
        dbutton.setEnabled(false);
        TextListener tlist = new TextListener() {
            public void textChanged (TextEvent event) {
                try {
                    dbutton.setEnabled(
                        parseInt(scrip.getText()) > 0 ||
                        parseInt(coins.getText()) > 0);
                } catch (NumberFormatException e) {
                    dbutton.setEnabled(false);
                }
            }
        };
        scrip.addListener(tlist);
        coins.addListener(tlist);
        bcont.add(dbutton);
        
        return dcont;
    }
    
    /**
     * Unsubscribes from the gang object and stops listening.
     */
    protected void unsubscribeFromGang ()
    {
        if (_gangsub != null) {
            _gangsub.unsubscribe(_ctx.getDObjectManager());
            _gangsub = null;
            _gangobj = null;
        }
    }
    
    /**
     * Parses the specified string as an integer, allowing the empty
     * string to represent zero.
     */
    protected static int parseInt (String text)
    {
        return (text.length() == 0) ? 0 : Integer.parseInt(text);
    }
    
    /** Listens to changes in the gang object. */
    protected class GangListener
        implements AttributeChangeListener
    {
        // documentation inherited from interface AttributeChangeListener
        public void attributeChanged (AttributeChangedEvent event)
        {
            String name = event.getName();
            if (_coffers != null && (name.equals(GangObject.SCRIP) ||
                name.equals(GangObject.COINS))) {
                _coffers.setMoney(_gangobj.scrip, _gangobj.coins, true);
            }
        }
    }
    
    protected HideoutObject _hideoutobj;
    protected GangObject _gangobj;
    
    protected HackyTabs _tabs;
    protected BContainer _gtab, _mtab, _rtab, _htab, _stab;
    protected StatusLabel _status;
    
    protected SafeSubscriber<GangObject> _gangsub;
    protected GangListener _ganglist = new GangListener();
    
    protected MoneyLabel _coffers;
    
    /** Listens to the user object for changes in gang membership. */
    protected AttributeChangeListener _gtupdater =
        new AttributeChangeListener() {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(PlayerObject.GANG_OID)) {
                // always go back to the first tab
                _tabs.selectTab(0); 
                updateGangTab();
            }
        }
    };
    
    protected static final String[] TABS = { "gang", "members", "rankings", "history" };
    protected static final Rectangle TABS_RECT = new Rectangle(
        166, 585, 15+4*140, 66);
    protected static final Rectangle TAB_RECT = new Rectangle(
        188, 83, 800, 503);
}
