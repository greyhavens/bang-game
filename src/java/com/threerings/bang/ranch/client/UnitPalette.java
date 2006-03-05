//
// $Id$

package com.threerings.bang.ranch.client;

import java.util.Iterator;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;

/**
 * Displays a grid of units, one of which can be selected at any given
 * time.
 */
public class UnitPalette extends IconPalette
{
    public UnitPalette (
        BangContext ctx, Inspector inspector, int columns, int rows)
    {
        super(inspector, columns, rows, UnitIcon.ICON_SIZE, 1);
        setPaintBackground(true);
        _ctx = ctx;
    }

    /**
     * Configures the palette to display the specified units.
     */
    public void setUnits (UnitConfig[] units)
    {
        for (int ii = 0; ii < units.length; ii++) {
            String name = _ctx.xlate(BangCodes.UNITS_MSGS, units[ii].getName());
            addIcon(createIcon(_ctx, -1, units[ii], name));
        }
    }

    /**
     * Configures the palette to display the supplied user's Big Shots.
     */
    public void setUser (PlayerObject user)
    {
        // listen to the user object for inventory additions and deletions
        _user = user;
        _user.addListener(_invlistener);

        // add icons for all existing big shots
        for (Iterator iter = user.inventory.iterator(); iter.hasNext(); ) {
            Object item = iter.next();
            if (item instanceof BigShotItem) {
                addUnit((BigShotItem)item);
            }
        }
    }

    /**
     * Returns the selected unit or null if none is selected.
     */
    public UnitIcon getSelectedUnit ()
    {
        return (UnitIcon)getSelectedIcon();
    }

    /**
     * Selects the unit with the specified item id.
     */
    public void selectUnit (int itemId)
    {
        for (SelectableIcon sicon : _icons) {
            UnitIcon icon = (UnitIcon)sicon;
            if (icon.getItemId() == itemId) {
                icon.setSelected(true);
                return;
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

    protected void addUnit (BigShotItem unit)
    {
        UnitConfig config = UnitConfig.getConfig(unit.getType());
        addIcon(createIcon(_ctx, unit.getItemId(), config,
                           unit.getName().toString()));
    }

    protected void removeUnit (int itemId)
    {
        for (SelectableIcon sicon : _icons) {
            UnitIcon icon = (UnitIcon)sicon;
            if (icon.getItemId() == itemId) {
                removeIcon(icon);
                return;
            }
        }
    }

    protected UnitIcon createIcon (
        BangContext ctx, int itemId, UnitConfig config, String name)
    {
        return new UnitIcon(ctx, itemId, config, name);
    }

    protected SetListener _invlistener = new SetListener() {
        public void entryAdded (EntryAddedEvent event) {
            if (event.getName().equals(PlayerObject.INVENTORY)) {
                Object item = event.getEntry();
                if (item instanceof BigShotItem) {
                    addUnit((BigShotItem)item);
                }
            }
        }
        public void entryUpdated (EntryUpdatedEvent event) {
            // nada
        }
        public void entryRemoved (EntryRemovedEvent event) {
            if (event.getName().equals(PlayerObject.INVENTORY)) {
                // removal of non-bigshots will just NOOP
                removeUnit((Integer)event.getKey());
            }
        }
    };

    protected BangContext _ctx;
    protected PlayerObject _user;
}
