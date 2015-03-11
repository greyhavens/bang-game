//
// $Id$

package com.threerings.bang.gang.client;

import com.jmex.bui.BButton;
import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BMenuItem;
import com.jmex.bui.BPopupMenu;
import com.jmex.bui.BToggleButton;
import com.jmex.bui.Spacer;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;
import com.jmex.bui.util.Point;
import com.jmex.bui.util.Rectangle;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ObjectAccessException;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.bang.client.MoneyLabel;
import com.threerings.bang.client.ShopView;
import com.threerings.bang.client.TownButton;
import com.threerings.bang.client.WalletLabel;
import com.threerings.bang.client.bui.EnablingValidator;
import com.threerings.bang.client.bui.HackyTabs;
import com.threerings.bang.client.bui.RequestDialog;
import com.threerings.bang.client.bui.StatusLabel;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.DeploymentConfig;

import com.threerings.bang.saloon.client.TableGameView;
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
    implements ActionListener, HideoutCodes, GangCodes
{
    public HideoutView (BangContext ctx)
    {
        super(ctx, HIDEOUT_MSGS);

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
            new Rectangle(177, 69, 391, 29));

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

    // documentation inherited from interface ActionListener
    public void actionPerformed (ActionEvent event)
    {
        String action = event.getAction();
        if (action.equals("options")) {
            displayOptionsMenu();
        } else if (action.equals("userOptions")) {
            displayUserOptionsMenu();
        } else if (action.equals("edit_buckle")) {
            _ctx.getBangClient().displayPopup(
                new BuckleDialog(_ctx, _hideoutobj, _gangobj), true, 500);
        } else if (action.equals("purchase_outfits")) {
            _ctx.getBangClient().displayPopup(
                new OutfitDialog(_ctx, _hideoutobj, _gangobj), true, 500);
        } else if (action.equals("inventory")) {
            _ctx.getBangClient().displayPopup(
                new GangInventory(_ctx, _hideoutobj, _gangobj), true, 500);
        } else if (action.equals("purchase_items")) {
            _ctx.getBangClient().displayPopup(
                new GangStoreDialog(_ctx, _hideoutobj, _gangobj), true, 500);
        } else if (action.equals("member_broadcast")) {
            displayBroadcastDialog();
        } else if (action.equals("history")) {
            _ctx.getBangClient().displayPopup(new HistoryDialog(_ctx, _hideoutobj), false, 500);
        } else if (action.equals("leave")) {
            leaveGang();
        }
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
                log.warning("Failed to subscribe to gang object", "oid", oid, "cause", cause);
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
        _bcont.removeAll();
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
            NON_MEMBER_TABS, true, 145, 15), NON_MEMBER_TABS_RECT);
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

        // add the buttons
        if (_ctx.getUserObject().gangRank == LEADER_RANK) {
            _bcont.add(_options = createButton("options"));
        } else {
            _bcont.add(_options = createButton("userOptions"));
        }
        _bcont.add(createButton("history"));
        _bcont.add(createButton("leave"));

        // add the tabs and gang chat (the first selected tab)
        _gcview = new GangChatView(_ctx, _gangobj, _status);
        add(_tabs = new HackyTabs(_ctx, false, "ui/hideout/tab_",
            MEMBER_TABS, true, 145, 15) {
            protected void tabSelected (int index) {
                _tcont.removeAll();
                if (index == 0) {
                    _tcont.add(_gcview);

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
        }, MEMBER_TABS_RECT);
    }

    @Override // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();
        if (_gcview != null) {
            _gcview.shutdown();
        }
    }

    /**
     * Creates a button with the supplied action.
     */
    protected BButton createButton (String action)
    {
        return new BButton(_msgs.get("m." + action), this, action);
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

    /**
     * Pops up the leader options menu.
     */
    protected void displayOptionsMenu ()
    {
        BPopupMenu menu = new BPopupMenu(getWindow(), false);
        menu.addMenuItem(new BMenuItem(_msgs.get("m.edit_buckle"), "edit_buckle"));
        if (DeploymentConfig.usesCoins()) {
            menu.addMenuItem(new BMenuItem(_msgs.get("m.purchase_outfits"), "purchase_outfits"));
        }
        menu.addMenuItem(new BMenuItem(_msgs.get("m.inventory"), "inventory"));
        menu.addMenuItem(new BMenuItem(_msgs.get("m.purchase_items"), "purchase_items"));
        menu.addMenuItem(new BMenuItem(_msgs.get("m.member_broadcast"), "member_broadcast"));
        menu.addListener(this);

        menu.popup(_options.getAbsoluteX(), _options.getAbsoluteY() + _options.getHeight(), true);
    }

    /**
     * Pops up the user options menu.
     */
    protected void displayUserOptionsMenu ()
    {
        BPopupMenu menu = new BPopupMenu(getWindow(), false);
        menu.addMenuItem(new BMenuItem(_msgs.get("m.show_buckle"), "edit_buckle"));
        menu.addMenuItem(new BMenuItem(_msgs.get("m.inventory"), "inventory"));
        menu.addListener(this);

        menu.popup(_options.getAbsoluteX(), _options.getAbsoluteY() + _options.getHeight(), true);
    }

    /**
     * Pops up the dialog that lets leaders broadcast to all online members.
     */
    protected void displayBroadcastDialog ()
    {
        _ctx.getBangClient().displayPopup(
            new RequestDialog(_ctx, HIDEOUT_MSGS, "m.broadcast_tip", "m.send",
                "m.cancel", "m.sent", _status) { {
                    setRequiresString(350, "");
                    _input.setMaxLength(MAX_BROADCAST_LENGTH);
                    new EnablingValidator(_input, _buttons[0]);
                }
                protected void fireRequest (Object result) {
                    _hideoutobj.service.broadcastToMembers((String)result, this);
                }
            }, true, 400);
    }

    /**
     * Pops up a confirmation making sure that the user really wants to leave the gang.
     */
    protected void leaveGang ()
    {
        String confirm = MessageBundle.tcompose("m.confirm_leave", _gangobj.name),
            success = MessageBundle.tcompose("m.left", _gangobj.name);
        _ctx.getBangClient().displayPopup(
            new RequestDialog(_ctx, HIDEOUT_MSGS, confirm, "m.ok", "m.cancel", success, _status) {
                protected void fireRequest (Object result) {
                    _hideoutobj.service.leaveGang(this);
                }
            }, true, 400);
    }

    /** Handles the menu for games, the member roster, and the gang directory. */
    protected class GangMenu extends BContainer
        implements ActionListener
    {
        public GangMenu ()
        {
            super(GroupLayout.makeVert(GroupLayout.CENTER).setOffAxisJustification(
                        GroupLayout.CENTER));
            setPreferredSize(new Dimension(494, -1));

            BContainer bcont = new BContainer(GroupLayout.makeHoriz(
                GroupLayout.NONE, GroupLayout.CENTER, GroupLayout.NONE));
            _buttons = new BToggleButton[] {
                addButton(bcont, "play"),
                addButton(bcont, "roster"),
                addButton(bcont, "directory"),
                addButton(bcont, "table") };
            add(bcont);

            _buttons[0].setSelected(true);
            add(_play = new PlayView(_ctx, _hideoutobj, _bcont, _buttons[3], _status));
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
                    _roster = new RosterView(_ctx, _hideoutobj, _gangobj, _status);
                }
                add(_roster);

            } else if (action.equals("table")) {
                if (_table == null) {
                    _table = new TableView(_ctx, _status, _bcont, _buttons[0]);
                    _table.willEnterPlace(_gangobj.tableOid);
                }
                add(_table);

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

        @Override // documentation inherited
        protected void wasRemoved ()
        {
            super.wasRemoved();
            _play.shutdown();
            if (_table != null) {
                _table.didLeavePlace();
            }
        }

        protected BToggleButton[] _buttons;
        protected PlayView _play;
        protected RosterView _roster;
        protected DirectoryView _directory;
        protected TableGameView _table;
    }

    protected HideoutObject _hideoutobj;
    protected GangObject _gangobj;

    protected HackyTabs _tabs;
    protected BContainer _ccont, _tcont, _bcont;
    protected BButton _options;
    protected GangChatView _gcview;

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

    /** The bounds of the non-member tabs. */
    protected static final Rectangle NON_MEMBER_TABS_RECT = new Rectangle(717, 588, 145, 44);

    /** The tabs for gang members. */
    protected final String[] MEMBER_TABS = { "gang_chat", "top_gangs", "top_members" };

    /** The bounds of the member tabs. */
    protected static final Rectangle MEMBER_TABS_RECT = new Rectangle(572, 588, 3*145, 44);
}
