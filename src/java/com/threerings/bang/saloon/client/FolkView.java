//
// $Id$

package com.threerings.bang.saloon.client;

import static com.threerings.bang.Log.log;

import java.util.HashMap;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BMenuItem;
import com.jmex.bui.BPopupMenu;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.BWindow;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.bang.chat.client.TabbedChatView;
import com.threerings.bang.client.InvitePardnerDialog;
import com.threerings.bang.client.PlayerService;
import com.threerings.bang.client.WantedPosterView;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BangOccupantInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.game.data.GameCodes;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;
import com.threerings.bang.util.BangContext;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;
import com.threerings.util.MessageBundle;

/**
 * Displays what friendly folks are present, and lets us chat with them.
 */
public class FolkView extends BContainer
    implements SetListener, AttributeChangeListener, ElementUpdateListener
{
    public FolkView (BangContext ctx, PaperView paper, SaloonObject salobj)
    {
        super(GroupLayout.makeVert(
            GroupLayout.STRETCH, GroupLayout.TOP, GroupLayout.STRETCH));
        setStyleClass("folk_view");
        _ctx = ctx;
        _paper = paper;
        _salobj = salobj;
        _user = ctx.getUserObject();

        BContainer folkListBox = new BContainer(new BorderLayout());
        folkListBox.setStyleClass("folk_list_box");
        add(folkListBox, GroupLayout.FIXED);

        // add the list header
        BLabel label = new BLabel(
            _ctx.xlate(SaloonCodes.SALOON_MSGS, "m.folk_present"),
            "folk_list_title");
        folkListBox.add(label, BorderLayout.NORTH);

        // and the dynamic list
        TableLayout listLayout = new TableLayout(2, 10, 4);
        listLayout.setEqualRows(true);
        _folkList = new BContainer(listLayout);
        folkListBox.add(new BScrollPane(_folkList), BorderLayout.CENTER);

        // and finally create (but do not add) the chat interface
        _folkTabs = new PardnerChatTabs(ctx);
	}
    
    // from interface SetListener
    public void entryAdded (EntryAddedEvent eae)
    {
        if (PlayerObject.PARDNERS.equals(eae.getName())) {
            PardnerEntry entry = (PardnerEntry) eae.getEntry();
            // if our new pardner is here with us, add to display
            if (_salobj.getOccupantInfo((Handle) entry.getKey()) != null) {
                new FolkCell(entry.handle, true).insertCell();
            }

        } else if (SaloonObject.OCCUPANT_INFO.equals(eae.getName())) {
            BangOccupantInfo info = (BangOccupantInfo) eae.getEntry();
            // if the new occupant is a friend of pardner, add to display
            boolean pard = _user.pardners.containsKey(info.username); 
            if (pard || _user.isFriend(info.playerId)) {                    
                new FolkCell((Handle) info.username, pard).insertCell();
            }
        }
    }
    
    // from interface SetListener
    public void entryRemoved (EntryRemovedEvent ere)
    {
        if (PlayerObject.PARDNERS.equals(ere.getName())) {
            PardnerEntry entry = (PardnerEntry) ere.getOldEntry();
            FolkCell cell = _folks.get(entry.handle);
            if (cell != null) {
                // this is pretty rare, let's just redraw the list
                recomputeList();
            }
            
        } else if (SaloonObject.OCCUPANT_INFO.equals(ere.getName())) {
            FolkCell cell = _folks.get(
                ((BangOccupantInfo) ere.getOldEntry()).username);
            if (cell != null) {
                cell.removeCell();
            }
        }
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent eue)
    {
        // we are unaffected by updates
    }

    // from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (PlayerObject.FRIENDS.equals(event.getName())) {
            recomputeList();
        }
    }

    // from interface ElementUpdateListener
    public void elementUpdated (ElementUpdatedEvent event)
    {
        if (PlayerObject.FRIENDS.equals(event.getName())) {
            recomputeList();
        }
    }

    @Override // from BContainer
    protected void wasAdded ()
    {
        super.wasAdded();
        
        // register as a listener for friends and pardners updates
        _user.addListener(this);
        // register as a listener for saloon occupant updates
        _salobj.addListener(this);
        // fill the list with initial data
        recomputeList();
    }


    @Override // from BContainer
    protected void wasRemoved ()
    {
        super.wasRemoved();
        
        _user.removeListener(this);
        _salobj.removeListener(this);
    }

    /** Display the chat interface */
    protected void showTabs ()
    {
        if (_folkTabs.getParent() == null) {
            add(_folkTabs);
        }
    }

    /** Hide the chat interface */
    protected void hideTabs ()
    {
        if (_folkTabs.getParent() != null) {
            remove(_folkTabs);
        }
    }

    /** Clear the list and recreate it from scratch */
    protected void recomputeList ()
    {
        _folkList.removeAll();
        _folks.clear();
        for (PardnerEntry entry : _user.pardners) {
            // list any pardner who is in our saloon
            if (_salobj.getOccupantInfo(entry.handle) != null) {
                new FolkCell(entry.handle, true).insertCell();
            }
        }
        for (OccupantInfo info : _salobj.occupantInfo) {
            // if they're our friend but not yet listed, do list them
            if (_user.isFriend(((BangOccupantInfo) info).playerId) &&
                !_folks.containsKey(info.username)) {
                new FolkCell((Handle) info.username, false).insertCell();
            }
        }
    }

    /** Represents one player in the friendly folks list */
    protected class FolkCell extends BContainer
        implements Comparable<FolkCell>
    {
        public FolkCell (Handle handle, boolean isPardner)
        {
            super(GroupLayout.makeHoriz(GroupLayout.LEFT));
            setPreferredSize(new Dimension(200, 18));
            _handle = handle;
            _isPardner = isPardner;

            add(new BLabel(_handle.toString(), "folk_label"));
            if (_isPardner) {
                BLabel pardIcon = new BLabel("", "folk_pardner");
                pardIcon.setIcon(
                    new ImageIcon(_ctx.loadImage(ParlorList.PARDS_PATH)));
                add(pardIcon);
            }
        }
        
        @Override // from BComponent
        public boolean dispatchEvent (BEvent event) {
            if (event instanceof MouseEvent) {
                MouseEvent mev = (MouseEvent)event;
                if (mev.getType() == MouseEvent.MOUSE_PRESSED) {
                    FolkPopupMenu menu =
                        new FolkPopupMenu(getWindow(), _handle);
                    menu.popup(mev.getX(), mev.getY(), false);
                    return true;
                }
            }
            return super.dispatchEvent(event);
        }


        /** Looks up and returns the proper avatar information for a player */
        public int[] getAvatar ()
        {
            if (_isPardner) {
                PardnerEntry entry = _user.pardners.get(_handle);
                return entry != null ? entry.avatar : null;
            } else {
                OccupantInfo info = _salobj.getOccupantInfo(_handle);
                return info != null ? ((BangOccupantInfo) info).avatar : null;
            }
        }
        
        // from interface Comparable
        public int compareTo (FolkCell cell)
        {
            if (_isPardner && !cell._isPardner) {
                return -1;
            }
            if (cell._isPardner && !_isPardner) {
                return 1;
            }
            return _handle.compareTo(cell._handle);
        }
        
        @Override // from Object
        public boolean equals (Object other)
        {
            if (other == null || !other.getClass().equals(getClass())) {
                return false;
            }
            return _isPardner == ((FolkCell) other)._isPardner &&
                _handle.equals(((FolkCell) other)._handle);
        }

        @Override //from Object
        public int hashCode ()
        {
            return (_handle.hashCode() << 1) + (_isPardner ? 1 : 0);
        }
        
        /**
         * A popup menu that is displayed when a player right clicks on an entry
         * in the friendly folks list in the Saloon.
         */
        protected class FolkPopupMenu extends BPopupMenu
            implements ActionListener
        {
            /**
             * Creates a popup menu for the specified player.
             */
            public FolkPopupMenu (BWindow parent, Handle handle)
            {
                super(parent);
        
                setStyleClass("folk_menu");
                addListener(this);
        
                MessageBundle bangMsgs = _ctx.getMessageManager().getBundle(
                    BangCodes.BANG_MSGS);
                MessageBundle saloonMsgs = _ctx.getMessageManager().getBundle(
                    SaloonCodes.SALOON_MSGS);
        
                // add their name as a non-menu item
                String title = "@=u(" + handle.toString() + ")";
                add(new BLabel(title, "player_menu_title"));
        
                addMenuItem(new BMenuItem(
                    saloonMsgs.get("m.folk_chat"), "chat"));
                
                // add an item for viewing their wanted poster
                addMenuItem(new BMenuItem(
                    bangMsgs.get("m.pm_view_poster"), "view_poster"));
        
                // add an item for muting/unmuting
                String mute = _ctx.getMuteDirector().isMuted(handle) ?
                    "unmute" : "mute";
                addMenuItem(new BMenuItem(bangMsgs.get("m.pm_" + mute), mute));

                if (!_isPardner) {
                    // add an item for removing them from the list
                    addMenuItem(new BMenuItem(
                        saloonMsgs.get("m.folk_remove"), "remove"));
                    // add an item for inviting them to be our pardner
                    addMenuItem(new BMenuItem(
                        bangMsgs.get("m.pm_invite_pardner"), "invite_pardner"));
                }
            }
        
            // from interface ActionListener
            public void actionPerformed (ActionEvent event)
            {
                if ("chat".equals(event.getAction())) {
                    _folkTabs.openUserTab(_handle, getAvatar(), true);

                } else if ("remove".equals(event.getAction())) {
                    OccupantInfo info = _salobj.getOccupantInfo(_handle);
                    if (info == null) {
                        // shouldn't happen
                        return;
                    }
                    int playerId = ((BangOccupantInfo) info).playerId; 
                    InvocationService.ConfirmListener listener =
                        new InvocationService.ConfirmListener() {
                        public void requestProcessed () {
                            // TODO: confirmation?
                        }
                        public void requestFailed(String cause) {
                            log.warning("Folk request failed: " + cause);
                            _ctx.getChatDirector().displayFeedback(
                                GameCodes.GAME_MSGS,
                                MessageBundle.tcompose(
                                    "m.folk_note_failed", cause));
                        }
                    };
                    PlayerService psvc = (PlayerService)
                        _ctx.getClient().requireService(PlayerService.class);
                    psvc.noteFolk(
                        _ctx.getClient(), playerId, PlayerService.FOLK_NEUTRAL,
                        listener);

                } else if ("mute".equals(event.getAction())) {
                    _ctx.getMuteDirector().setMuted(_handle, true);
        
                } else if ("unmute".equals(event.getAction())) {
                    _ctx.getMuteDirector().setMuted(_handle, false);
        
                } else if ("invite_pardner".equals(event.getAction())) {
                    _ctx.getBangClient().displayPopup(
                        new InvitePardnerDialog(_ctx, _handle), true, 400);
        
                } else if ("view_poster".equals(event.getAction())) {
                    WantedPosterView.displayWantedPoster(_ctx, _handle);
                }
            }
        }
        
        /**
         * Insert a FolkCell into the friendly folks list at the right
         * position as determined by compareTo(). If a cell associated
         * with the same handle was already in the list, it is replaced.
         */
        protected void insertCell ()
        {
            FolkCell oldCell = _folks.put(_handle, this);
            if (oldCell != null) {
                _folkList.remove(oldCell);
            }
            for (int i = 0; i < _folkList.getComponentCount(); i ++) {
                FolkCell other = (FolkCell) _folkList.getComponent(i);
                if (compareTo(other) < 0) {
                    continue;
                }
                _folkList.add(i, this);
                return;
            }
            _folkList.add(this);
        }

        /** Remove a FolkCell from the friendly folks list */
        protected void removeCell ()
        {
            _folks.remove(_handle);
            _folkList.remove(this);
        }

		/** The handle of the player this cell represents */
        protected Handle _handle;
        /** Whether or not this cell's player is our pardner */
        protected boolean _isPardner;
    }

    /** A subclass that knows how to show and hide the chat tabs */
    protected class PardnerChatTabs extends TabbedChatView
    {
    
        public PardnerChatTabs (BangContext ctx)
        {
            super(ctx, TAB_DIMENSION);
        }
    
        @Override // from TabbedChatView
        protected boolean displayTabs ()
        {
            if (!isAdded()) {
                showTabs();
            }
            _paper.folkChatAlert();
            return true;
        }
        
        @Override // from TabbedChatView
        protected void lastTabClosed ()
        {
            hideTabs();
        }
        
    }

    /** The hard-coded dimensions of the chat tabs */
    protected final static Dimension TAB_DIMENSION = new Dimension(400, 140);

    /** Maps handles of displayed people to their display cells */
    protected HashMap<Handle, FolkCell> _folks =
        new HashMap<Handle, FolkCell>();

    /** The table containing the actual FolkCell components */
    protected BContainer _folkList;
    
    /** The tabbed chat view for our folks */
    protected PardnerChatTabs _folkTabs;
    
    /** A reference to our player object */
    protected PlayerObject _user;
    
    /** A reference to our context */
    protected BangContext _ctx;

    /** A reference to the paper view in which we are but one page */
    protected PaperView _paper;
    
    /** A reference to the saloon we're in */
    protected SaloonObject _salobj;
}
