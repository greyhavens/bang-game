//
// $Id$

package com.threerings.bang.saloon.client;

import java.util.HashMap;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.threerings.bang.data.BangOccupantInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;
import com.threerings.bang.util.BangContext;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

/**
 * Displays what friendly folks are present, and lets us chat with them.
 */
public class FolkView extends BContainer
    implements SetListener, AttributeChangeListener, ElementUpdateListener
{
    public FolkView (BangContext ctx, SaloonObject salobj)
    {
        super(GroupLayout.makeVert(GroupLayout.TOP));
        setStyleClass("folk_view");
        _ctx = ctx;
        _salobj = salobj;
        _user = ctx.getUserObject();

        BContainer folkListBox = new BContainer(new BorderLayout());
        folkListBox.setStyleClass("folk_list_box");
        add(folkListBox);

        folkListBox.add(new BLabel(
                _ctx.xlate(SaloonCodes.SALOON_MSGS, "m.folk_present"),
                "folk_list_title"),
            BorderLayout.NORTH);

        TableLayout layout = new TableLayout(2, 10, 4);
        layout.setEqualRows(true);
        _folkList = new BContainer(layout);
        folkListBox.add(new BScrollPane(_folkList), BorderLayout.CENTER);

        // TODO: add chat view here
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
    protected void wasRemoved()
    {
        super.wasRemoved();
        
        _user.removeListener(this);
        _salobj.removeListener(this);
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
                (Handle) ((BangOccupantInfo) ere.getOldEntry()).username);
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
                !_folks.containsKey((Handle) info.username)) {
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
            super(new BorderLayout(10, 0));
            setStyleClass("folk_entry");
            _handle = handle;
            _isPardner = isPardner;

            add(new BLabel(_handle.toString(), "parlor_label"),
                BorderLayout.CENTER);
            if (_isPardner) {
                BLabel pardIcon = new BLabel("");
                pardIcon.setIcon(
                    new ImageIcon(_ctx.loadImage(ParlorList.PARDS_PATH)));
                add(pardIcon, BorderLayout.EAST);
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
    
    /** Maps handles of displayed people to their display cells */
    protected HashMap<Handle, FolkCell> _folks =
        new HashMap<Handle, FolkCell>();

    /** The table containing the actual FolkCell components */
    protected BContainer _folkList;
    
    /** A reference to our player object */
    protected PlayerObject _user;
    
    /** A reference to our context */
    protected BangContext _ctx;
    
    /** A reference to the saloon we're in */
    protected SaloonObject _salobj;
}
