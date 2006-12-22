//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BToggleButton;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.AbsoluteLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;

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
    implements GangCodes
{
    public HideoutView (BangContext ctx)
    {
        super(ctx, HideoutCodes.HIDEOUT_MSGS);

        // add our various interface components
        add(new BLabel(_msgs.get("m.intro_tip"), "shop_status"),
            new Rectangle(232, 656, 570, 35));

        add(new WalletLabel(_ctx, true), new Rectangle(25, 40, 150, 40));
        add(createHelpButton(), new Point(780, 25));
        add(new TownButton(ctx), new Point(870, 25));
        add(_status = new StatusLabel(ctx), new Rectangle(250, 10, 520, 50));
        _status.setStyleClass("shop_status");
        GroupLayout glay = GroupLayout.makeVert(
            GroupLayout.NONE, GroupLayout.TOP, GroupLayout.NONE);
        glay.setOffAxisJustification(GroupLayout.RIGHT);
        add(_ccont = new BContainer(glay), new Rectangle(61, 113, 494, 529));
        add(_tcont = new BContainer(GroupLayout.makeVStretch()), new Rectangle(576, 72, 427, 515));
        add(_bcont = new BContainer(GroupLayout.makeHoriz(GroupLayout.RIGHT)),
            new Rectangle(277, 69, 291, 29));
        
        // start with a random shop tip
        _status.setStatus(getShopTip(), false);
    }

    // documentation inherited from interface PlaceView
    public void willEnterPlace (PlaceObject plobj)
    {
        _hideoutobj = (HideoutObject)plobj;
        
        // subscribe to the gang object and update the ui
        updateGangObject();
        
        // listen for changes in gang membership
        _ctx.getUserObject().addListener(_userlist);
    }
    
    // documentation inherited from interface PlaceView
    public void didLeavePlace (PlaceObject plobj)
    {
        // stop listening to the user
        _ctx.getUserObject().removeListener(_userlist);
        
        // unsubscribe from the gang object
        unsubscribeFromGang();
    }
    
    @Override // documentation inherited
    protected Point getShopkeepNameLocation ()
    {
        return new Point(22, 554);
    }
    
    /**
     * Updates the UI when first entering or when the player joins or leaves a gang.
     */
    protected void updateGangObject ()
    {
        // get rid of the old interface, if any
        if (_tabs != null) {
            clearInterface();
        }
        
        // if the user is not in a gang, make sure that we are not subscribed
        PlayerObject player = _ctx.getUserObject();
        if (player.gangOid <= 0) {
            unsubscribeFromGang();
            populateNonMemberInterface();
            return;
        }
        
        // subscribe to the gang object
        _status.setStatus(_msgs.get("m.loading_gang"), false);
        (_gangsub = new SafeSubscriber<GangObject>(
            player.gangOid, new Subscriber<GangObject>() {
            public void objectAvailable (GangObject gangobj) {
                _gangobj = gangobj;
                _ctx.getChatDirector().addAuxiliarySource(_gangobj, ChatCodes.PLACE_CHAT_TYPE);
                populateMemberInterface();
                _status.setStatus(getShopTip(), false);
            }
            public void requestFailed (int oid, ObjectAccessException cause) {
                log.warning("Failed to subscribe to gang object [oid=" + oid +
                    ", cause=" + cause + "].");
                _status.setStatus(_msgs.get("m.internal_error"), true);
            }
        })).subscribe(_ctx.getDObjectManager());
    }
    
    /**
     * Clears out the entire interface.
     */
    protected void clearInterface ()
    {
        remove(_tabs);
        _ccont.removeAll();
        _tcont.removeAll();
    }
    
    /**
     * Populates the interface for non-members.
     */
    protected void populateNonMemberInterface ()
    {
        // add the about gangs/start gang components
        _ccont.add(new BLabel(new ImageIcon(
            _ctx.loadImage("ui/hideout/title_about_gangs.png")), "about_gangs_title"));
        _ccont.add(new BLabel(_msgs.get("m.about_gangs"), "about_gangs"));
        _ccont.add(new BLabel(_msgs.get("m.start_gang"), "start_gang"));
        _ccont.add(new BLabel(_msgs.get("m.start_gang_reqs"), "start_gang_reqs"));
        BContainer bcont = GroupLayout.makeHBox(GroupLayout.RIGHT);
        bcont.add(new BLabel(_msgs.get("m.fee"), "start_gang_cost"));
        MoneyLabel flabel = new MoneyLabel(_ctx);
        flabel.setMoney(FORM_GANG_SCRIP_COST, FORM_GANG_COIN_COST, false);
        flabel.setStyleClass("start_gang_cost");
        bcont.add(flabel);
        bcont.add(new BButton(_msgs.get("b.start_gang"), new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                FormGangDialog.show(_ctx, _status, _hideoutobj.service);
            }
        }, "start_gang"));
        bcont.add(new Spacer(20, 1));
        _ccont.add(bcont);
        
        // add the gang directory
        _ccont.add(new BLabel(new ImageIcon(
            _ctx.loadImage("ui/hideout/title_gang_directory.png")), "gang_directory_title"));
        _ccont.add(new DirectoryView(_ctx, _hideoutobj));
        
        // add the one selectable tab
        add(_tabs = new HackyTabs(_ctx, false, "ui/hideout/tab_",
            NON_MEMBER_TABS, true, 145, 15), TABS_RECT);
        _tcont.add(new TopGangView(_ctx, _hideoutobj));
    }
    
    /**
     * Populates the interface for gang members.
     */
    protected void populateMemberInterface ()
    {
        // add the gang info view
        _ccont.add(new GangInfoView(_ctx, _hideoutobj, _gangobj, _status));
        
        // add the gang menu
        _ccont.add(new GangMenu());
        
        // add the tabs and gang chat (the first selected tab)
        final GangChatView gcview = new GangChatView(_ctx, _hideoutobj, _gangobj, _status);
        add(_tabs = new HackyTabs(_ctx, false, "ui/hideout/tab_",
            MEMBER_TABS, true, 145, 15) {
            protected void tabSelected (int index) {
                _tcont.removeAll();
                if (index == 0) {
                    _tcont.add(gcview);
                    
                } else if (index == 1) {
                    if (_tgview == null) {
                        _tgview = new TopGangView(_ctx, _hideoutobj);
                    }
                    _tcont.add(_tgview);
                    
                } else { // index == 2
                    if (_tmview == null) {
                        _tmview = new TopScoreView(_ctx, _gangobj) {
                            protected String getHeaderText () {
                                return _msgs.get("m.top_members");
                            }
                        };
                        _tmview.setStyleClass("gang_rank_view");
                    }
                    _tcont.add(_tmview);
                }
            }
            protected TopGangView _tgview;
            protected TopScoreView _tmview;
        }, TABS_RECT);
    }
    
    /**
     * Unsubscribes from the gang object and stops listening.
     */
    protected void unsubscribeFromGang ()
    {
        if (_gangsub != null) {
            _gangsub.unsubscribe(_ctx.getDObjectManager());
            _gangsub = null;
        }
        if (_gangobj != null) {
            _ctx.getChatDirector().removeAuxiliarySource(_gangobj);
            _gangobj = null;
        }
    }
    
    /** Handles the menu for games, the member roster, and the gang directory. */
    protected class GangMenu extends BContainer
        implements ActionListener
    {
        public GangMenu ()
        {
            super(GroupLayout.makeVert(GroupLayout.CENTER));
            
            BContainer bcont = new BContainer(GroupLayout.makeHoriz(
                GroupLayout.NONE, GroupLayout.CENTER, GroupLayout.NONE));
            _buttons = new BToggleButton[] {
                addButton(bcont, "play"),
                addButton(bcont, "roster"),
                addButton(bcont, "directory") };
            add(bcont);
            
            _buttons[0].setSelected(true);
            add(_play = new PlayView(_ctx, _hideoutobj));
        }
        
        // documentation inherited from interface ActionListener
        public void actionPerformed (ActionEvent event)
        {
            Object src = event.getSource();
            for (BToggleButton button : _buttons) {
                if (button != src) {
                    button.setSelected(false);
                }
            }
            if (getComponentCount() > 1) {
                remove(1);
            }
            String action = event.getAction();
            if (action.equals("play")) {
                add(_play);
                
            } else if (action.equals("roster")) {
                if (_roster == null) {
                    _roster = new RosterView(_ctx, _hideoutobj, _gangobj, _bcont, _status);
                }
                add(_roster);
                
            } else { // action.equals("directory")
                if (_directory == null) {
                    _directory = new DirectoryView(_ctx, _hideoutobj);
                }
                add(_directory);
            }
        }
        
        protected BToggleButton addButton (BContainer bcont, String action)
        {
            BToggleButton button = new BToggleButton("", action) {
                protected void fireAction (long when, int modifiers) {
                    if (!_selected) { // only selection, no deselection
                        super.fireAction(when, modifiers);
                    }
                }
            };
            button.setStyleClass("menu_" + action);
            button.addListener(this);
            bcont.add(button);
            return button;
        }
        
        protected BToggleButton[] _buttons;
        protected PlayView _play;
        protected RosterView _roster;
        protected DirectoryView _directory;
    }
    
    protected HideoutObject _hideoutobj;
    protected GangObject _gangobj;
    
    protected HackyTabs _tabs;
    protected BContainer _ccont, _tcont, _bcont;
    
    protected StatusLabel _status;
    
    protected SafeSubscriber<GangObject> _gangsub;
    
    /** Listens to the user object for changes in gang membership. */
    protected AttributeChangeListener _userlist = new AttributeChangeListener() {
        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(PlayerObject.GANG_OID)) {
                updateGangObject();
            }
        }
    };
    
    /** The tabs for non-members. */
    protected final String[] NON_MEMBER_TABS = { "top_gangs" };
    
    /** The tabs for gang members. */
    protected final String[] MEMBER_TABS = { "gang_chat", "top_gangs", "top_members" };
    
    /** The bounds of the tabs. */
    protected static final Rectangle TABS_RECT = new Rectangle(572, 588, 3*145, 44);
}
