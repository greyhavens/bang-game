//
// $Id$

package com.threerings.bang.saloon.client;

import java.util.HashMap;

import com.jmex.bui.BContainer;
import com.jmex.bui.BLabel;
import com.jmex.bui.BScrollPane;
import com.jmex.bui.icon.ImageIcon;
import com.jmex.bui.layout.BorderLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.data.BangOccupantInfo;
import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.util.BangContext;

/**
 * Displays what folks are present, and lets us chat with them.
 */
public class FolkView extends BContainer
    implements SetListener<DSet.Entry>, AttributeChangeListener, ElementUpdateListener
{
    public FolkView (BangContext ctx, PlaceObject pobj, boolean ffOnly, boolean showGangMembership)
    {
        super(new BorderLayout());

        _ctx = ctx;
        _pobj = pobj;
        _user = ctx.getUserObject();
        _ffOnly = ffOnly;
        _showGangMembership = showGangMembership;

        TableLayout listLayout = new TableLayout(2, 2, 10);
        listLayout.setEqualRows(true);
        _folkList = new BContainer(listLayout);
        BScrollPane scrolly = new BScrollPane(_folkList);
        if (ffOnly) {
            scrolly.setStyleClass("folk_list_box");
            // we don't want to grow beyond the size of our background image
            setPreferredSize(new Dimension(434, 137));
        }
        add(scrolly, BorderLayout.CENTER);

    }

    // from interface SetListener
    public void entryAdded (EntryAddedEvent<DSet.Entry> eae)
    {
        if (PlayerObject.PARDNERS.equals(eae.getName())) {
            PardnerEntry entry = (PardnerEntry) eae.getEntry();
            // if our new pardner is here with us, add to display
            BangOccupantInfo info = (BangOccupantInfo)_pobj.getOccupantInfo(entry.handle);
            if (info != null) {
                maybeInsertCell(info);
            }

        } else if (PlaceObject.OCCUPANT_INFO.equals(eae.getName())) {
            // if the new occupant is a friend or pardner, add to display
            maybeInsertCell((BangOccupantInfo)eae.getEntry());
        }
    }

    // from interface SetListener
    public void entryRemoved (EntryRemovedEvent<DSet.Entry> ere)
    {
        if (PlayerObject.PARDNERS.equals(ere.getName())) {
            PardnerEntry entry = (PardnerEntry) ere.getOldEntry();
            FolkCell cell = _folks.get(entry.handle);
            if (cell != null) {
                // this is pretty rare, let's just redraw the list
                recomputeList();
            }

        } else if (PlaceObject.OCCUPANT_INFO.equals(ere.getName())) {
            FolkCell cell = _folks.get(
                ((BangOccupantInfo) ere.getOldEntry()).username);
            if (cell != null) {
                removeCell(cell);
            }
        }
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent<DSet.Entry> eue)
    {
        if (PlaceObject.OCCUPANT_INFO.equals(eue.getName())) {
            maybeInsertCell((BangOccupantInfo)eue.getEntry());
        }
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
        _pobj.addListener(this);

        // fill the list with initial data
        recomputeList();
    }

    @Override // from BContainer
    protected void wasRemoved ()
    {
        super.wasRemoved();

        // clear out our listeners
        _user.removeListener(this);
        _pobj.removeListener(this);
    }

    /**
     * Clears the Friendly Folks list and recreate it from scratch.
     */
    protected void recomputeList ()
    {
        _folkList.removeAll();
        _folks.clear();
        for (OccupantInfo info : _pobj.occupantInfo) {
            maybeInsertCell((BangOccupantInfo)info);
        }

        // if we added folks or this is not ff only, great, we're done
        if (_folkList.getComponentCount() > 0 || !_ffOnly) {
            return;
        }

        // if they have folks marked as friendly but none of them are around
        // just say there's no friendly folks in the saloon
        if (_user.friends.length > 0 || _user.foes.length > 0) {
            String nomsg = _ctx.xlate(SaloonCodes.SALOON_MSGS, "m.no_folks");
            _folkList.add(new BLabel(nomsg));
            return;
        }

        // otherwise give them a little howto on Friendly Folks
        BContainer hbox = new BContainer(new BorderLayout(20, 0));
        ImageIcon ff = new ImageIcon(_ctx.loadImage("ui/pstatus/folks/ff.png"));
        hbox.add(new BLabel(ff, "folk_howto_button"), BorderLayout.WEST);
        String msg = _ctx.xlate(SaloonCodes.SALOON_MSGS, "m.folks_howto");
        hbox.add(new BLabel(msg, "folk_howto"), BorderLayout.CENTER);
        _folkList.add(hbox);
    }

    /**
     * Inserts or updates a cell for the described occupant if appropriate.
     */
    protected void maybeInsertCell (BangOccupantInfo info)
    {
        boolean pard = _user.pardners.containsKey(info.username);
        boolean ff = _user.isFriend(info.playerId);
        if (pard || !_ffOnly || ff) {
            insertCell(new FolkCell(_ctx, (Handle)info.username, pard,
                _ffOnly && ff, _showGangMembership && info.gangId > 0));
        }
    }

    /**
     * Insert a FolkCell into the friendly folks list at the right position as
     * determined by compareTo(). If a cell associated with the same handle was
     * already in the list, it is replaced.
     */
    protected void insertCell (FolkCell cell)
    {
        // clear out the "no one's here" or "howto" display if necessary
        if (_folkList.getComponentCount() > 0 &&
            !(_folkList.getComponent(0) instanceof FolkCell)) {
            _folkList.remove(0);
        }

        FolkCell oldCell = _folks.put(cell._handle, cell);
        if (oldCell != null) {
            _folkList.remove(oldCell);
        }
        for (int ii = 0; ii < _folkList.getComponentCount(); ii ++) {
            FolkCell other = (FolkCell) _folkList.getComponent(ii);
            if (cell.compareTo(other) > 0) {
                continue;
            }
            _folkList.add(ii, cell);
            return;
        }
        _folkList.add(cell);
    }

    /** Removes a cell from the friendly folks list. */
    protected void removeCell (FolkCell cell)
    {
        _folks.remove(cell._handle);
        _folkList.remove(cell);
    }

    /** A reference to our context */
    protected BangContext _ctx;

    /** A reference to the place we're in */
    protected PlaceObject _pobj;

    /** A reference to our player object */
    protected PlayerObject _user;

    /** The table containing the actual FolkCell components */
    protected BContainer _folkList;

    /** Are we showing only friendly folks? */
    protected boolean _ffOnly;

    /** Are we indicating gang membership? */
    protected boolean _showGangMembership;

    /** Maps handles of displayed people to their display cells */
    protected HashMap<Handle, FolkCell> _folks = new HashMap<Handle, FolkCell>();
}
