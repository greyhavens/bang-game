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
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ElementUpdateListener;
import com.threerings.presents.dobj.ElementUpdatedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.chat.client.PlaceChatView;
import com.threerings.bang.client.bui.BangHTMLView;
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
    public FolkView (BangContext ctx, SaloonObject salobj, PlaceChatView chat)
    {
        super(new BorderLayout());

        _ctx = ctx;
        _salobj = salobj;
        _chat = chat;
        _user = ctx.getUserObject();

        TableLayout listLayout = new TableLayout(2, 2, 10);
        listLayout.setEqualRows(true);
        _folkList = new BContainer(listLayout);
        BScrollPane scrolly = new BScrollPane(_folkList);
        scrolly.setStyleClass("folk_list_box");
        add(scrolly, BorderLayout.CENTER);

        // we don't want to grow beyond the size of our background image
        setPreferredSize(new Dimension(434, 137));
    }

    // from interface SetListener
    public void entryAdded (EntryAddedEvent eae)
    {
        if (PlayerObject.PARDNERS.equals(eae.getName())) {
            PardnerEntry entry = (PardnerEntry) eae.getEntry();
            // if our new pardner is here with us, add to display
            if (_salobj.getOccupantInfo((Handle) entry.getKey()) != null) {
                insertCell(new FolkCell(_ctx, _chat, entry.handle, true));
            }

        } else if (SaloonObject.OCCUPANT_INFO.equals(eae.getName())) {
            BangOccupantInfo info = (BangOccupantInfo) eae.getEntry();
            // if the new occupant is a friend of pardner, add to display
            boolean pard = _user.pardners.containsKey(info.username);
            if (pard || _user.isFriend(info.playerId)) {
                Handle handle = (Handle)info.username;
                insertCell(new FolkCell(_ctx, _chat, handle, pard));
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

        // clear out our listeners
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
                insertCell(new FolkCell(_ctx, _chat, entry.handle, true));
            }
        }
        for (OccupantInfo info : _salobj.occupantInfo) {
            // if they're our friend but not yet listed, do list them
            if (_user.isFriend(((BangOccupantInfo) info).playerId) &&
                !_folks.containsKey(info.username)) {
                Handle handle = (Handle)info.username;
                insertCell(new FolkCell(_ctx, _chat, handle, false));
            }
        }

        // if we added folks, great, we're done
        if (_folkList.getComponentCount() > 0) {
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

    /** Removes a cell from the friendly folks list. */
    protected void removeCell (FolkCell cell)
    {
        _folks.remove(cell._handle);
        _folkList.remove(cell);
    }

    /** A reference to our context */
    protected BangContext _ctx;

    /** The chat view in which we display chat with folks */
    protected PlaceChatView _chat;

    /** A reference to the saloon we're in */
    protected SaloonObject _salobj;

    /** A reference to our player object */
    protected PlayerObject _user;

    /** The table containing the actual FolkCell components */
    protected BContainer _folkList;

    /** Maps handles of displayed people to their display cells */
    protected HashMap<Handle, FolkCell> _folks =
        new HashMap<Handle, FolkCell>();
}
