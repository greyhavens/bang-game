//
// $Id$

package com.threerings.bang.ranch.client;

import java.util.Iterator;

import com.jme.bui.BContainer;
import com.jme.bui.border.EmptyBorder;
import com.jme.bui.layout.TableLayout;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.data.BangUserObject;
import com.threerings.bang.data.BigShot;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

/**
 * Displays a grid of units, one of which can be selected at any given
 * time.
 */
public class UnitPalette extends BContainer
{
    public UnitPalette (BangContext ctx, UnitInspector inspector)
    {
        super(new TableLayout(4, 5, 5));
        setBorder(new EmptyBorder(5, 5, 5, 5));

        _ctx = ctx;
        _inspector = inspector;
    }

    /**
     * Configures the palette to display the specified units.
     */
    public void setUnits (UnitConfig[] units)
    {
        for (int ii = 0; ii < units.length; ii++) {
            add(new UnitIcon(_ctx, -1, units[ii]));
        }
    }

    /**
     * Configures the palette to display the supplied user's Big Shots.
     */
    public void setUser (BangUserObject user)
    {
        // listen to the user object for inventory additions and deletions
        _user = user;
        _user.addListener(_invlistener);

        // add icons for all existing big shots
        for (Iterator iter = user.inventory.iterator(); iter.hasNext(); ) {
            Object item = iter.next();
            if (item instanceof BigShot) {
                addUnit((BigShot)item);
            }
        }
    }

    /**
     * This must be called when the palette is going away to allow it to
     * remove its listener registrations.
     */
    public void shutdown ()
    {
        // remove our listener if we've configured one
        if (_user != null) {
            _user.removeListener(_invlistener);
            _user = null;
        }
    }

    protected void addUnit (BigShot unit)
    {
        UnitConfig config = UnitConfig.getConfig(unit.getType());
        add(new UnitIcon(_ctx, unit.getItemId(), config));
    }

    protected void removeUnit (int itemId)
    {
        for (int ii = 0; ii < getComponentCount(); ii++) {
            UnitIcon icon = (UnitIcon)getComponent(ii);
            if (icon.getItemId() == itemId) {
                remove(icon);
                return;
            }
        }
    }

    protected void iconSelected (UnitIcon icon)
    {
        // deselect all other icons
        for (int ii = 0; ii < getComponentCount(); ii++) {
            UnitIcon child = (UnitIcon)getComponent(ii);
            if (child != icon) {
                child.setSelected(false);
            }
        }

        // inspect this unit
        _inspector.setUnit(icon.getItemId(), icon.getUnit());
    }

    protected SetListener _invlistener = new SetListener() {
        public void entryAdded (EntryAddedEvent event) {
            if (event.getName().equals(BangUserObject.INVENTORY)) {
                Object item = event.getEntry();
                System.err.println("unit added");
                if (item instanceof BigShot) {
                    addUnit((BigShot)item);
                }
            }
        }
        public void entryUpdated (EntryUpdatedEvent event) {
            // nada
        }
        public void entryRemoved (EntryRemovedEvent event) {
            if (event.getName().equals(BangUserObject.INVENTORY)) {
                // removal of non-bigshots will just NOOP
                removeUnit((Integer)event.getKey());
            }
        }
    };

    protected BangContext _ctx;
    protected UnitInspector _inspector;
    protected BangUserObject _user;
    protected UnitIcon _selection;
}
