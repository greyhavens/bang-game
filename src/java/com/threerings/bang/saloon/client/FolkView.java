//
// $Id$

package com.threerings.bang.saloon.client;

import java.util.HashMap;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.chat.client.PlaceChatView;
import com.threerings.bang.data.BangOccupantInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;
import com.threerings.bang.util.BangContext;

import static com.threerings.bang.Log.log;

/**
 * Displays what friendly folks are present, and lets us chat with them.
 */
public class FolkView extends BContainer
    implements SetListener, AttributeChangeListener, ElementUpdateListener
{
    public FolkView (BangContext ctx, PaperView paper, SaloonObject salobj)
    {
        super(GroupLayout.makeVStretch());
        _ctx = ctx;
        _paper = paper;
        _salobj = salobj;
        _user = ctx.getUserObject();

        // and the dynamic list
        TableLayout listLayout = new TableLayout(2, 2, 10);
        listLayout.setEqualRows(true);
        _folkList = new BContainer(listLayout);
        BScrollPane scrolly = new BScrollPane(_folkList);
        scrolly.setStyleClass("folk_list_box");
        scrolly.setPreferredSize(new Dimension(434, 137));
        add(scrolly, GroupLayout.FIXED);

        // and finally create (but do not add) the chat interface
        add(_folkTabs = new PardnerChatTabs(ctx));
    }

    // from interface SetListener
    public void entryAdded (EntryAddedEvent eae)
    {
        if (PlayerObject.PARDNERS.equals(eae.getName())) {
            PardnerEntry entry = (PardnerEntry) eae.getEntry();
            // if our new pardner is here with us, add to display
            if (_salobj.getOccupantInfo((Handle) entry.getKey()) != null) {
                insertCell(new FolkCell(this, entry.handle, true));
            }

        } else if (SaloonObject.OCCUPANT_INFO.equals(eae.getName())) {
            BangOccupantInfo info = (BangOccupantInfo) eae.getEntry();
            // if the new occupant is a friend of pardner, add to display
            boolean pard = _user.pardners.containsKey(info.username);
            if (pard || _user.isFriend(info.playerId)) {
                insertCell(new FolkCell(this, (Handle) info.username, pard));
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
                removeCell(cell);
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

    /**
     * Clears the Friendly Folks list and recreate it from scratch.
     */
    protected void recomputeList ()
    {
        _folkList.removeAll();
        _folks.clear();
        for (PardnerEntry entry : _user.pardners) {
            // list any pardner who is in our saloon
            if (_salobj.getOccupantInfo(entry.handle) != null) {
                insertCell(new FolkCell(this, entry.handle, true));
            }
        }
        for (OccupantInfo info : _salobj.occupantInfo) {
            // if they're our friend but not yet listed, do list them
            if (_user.isFriend(((BangOccupantInfo) info).playerId) &&
                !_folks.containsKey(info.username)) {
                insertCell(new FolkCell(this, (Handle) info.username, false));
            }
        }
    }

    /**
     * Insert a FolkCell into the friendly folks list at the right position as
     * determined by compareTo(). If a cell associated with the same handle was
     * already in the list, it is replaced.
     */
    protected void insertCell (FolkCell cell)
    {
        FolkCell oldCell = _folks.put(cell._handle, cell);
        if (oldCell != null) {
            _folkList.remove(oldCell);
        }
        for (int ii = 0; ii < _folkList.getComponentCount(); ii ++) {
            FolkCell other = (FolkCell) _folkList.getComponent(ii);
            if (cell.compareTo(other) < 0) {
                continue;
            }
            _folkList.add(ii, cell);
            return;
        }
        _folkList.add(cell);
    }

    /** Remove a FolkCell from the friendly folks list */
    protected void removeCell (FolkCell cell)
    {
        _folks.remove(cell._handle);
        _folkList.remove(cell);
    }

    /** A subclass that knows how to show and hide the chat tabs */
    protected class PardnerChatTabs extends PlaceChatView
    {
        public PardnerChatTabs (BangContext ctx)
        {
            super(ctx, ctx.xlate(SaloonCodes.SALOON_MSGS, "m.saloon_chat"));
        }

        @Override // from TabbedChatView
        protected boolean displayTabs () {
            _paper.folkChatAlert();
            return true;
        }
    }

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
