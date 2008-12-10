//
// $Id$

package com.threerings.bang.ranch.client;

import com.jmex.bui.layout.GroupLayout;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.bang.client.bui.IconPalette;
import com.threerings.bang.client.bui.SelectableIcon;
import com.threerings.bang.data.BigShotItem;
import com.threerings.bang.data.Item;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.data.UnitConfig;
import com.threerings.bang.util.BangContext;
import com.threerings.bang.util.BangUtil;

/**
 * Displays a grid of units, one of which can be selected at any given
 * time.
 */
public class UnitPalette extends IconPalette
{
    public UnitPalette (BangContext ctx, Inspector inspector, int columns, int rows)
    {
        super(inspector, columns, rows, UnitIcon.ICON_SIZE, 1);
        setPaintBackground(true);
        _ctx = ctx;

        // sneakily squeeze our back and more buttons closer together; my
        // kingdom for artists that can make UIs with consistent dimensions
        ((GroupLayout)_bcont.getLayoutManager()).setGap(15);
    }

    /**
     * Configures the palette to display the specified units.
     */
    public void setUnits (UnitConfig[] units, boolean disableUnavail)
    {
        for (int ii = 0; ii < units.length; ii++) {
            UnitIcon icon = new UnitIcon(_ctx, units[ii]);
            icon.displayAvail(_ctx, disableUnavail);
            addIcon(icon);
        }
    }

    /**
     * Configures the palette to display the specified Big Shot units,
     * substituting the player's recruited Big Shots where available and
     * marking other Big Shots as "locked".
     */
    public void setBigShots (UnitConfig[] units, PlayerObject user)
    {
        // listen to the user object for inventory additions
        _user = user;
        _user.addListener(_invlistener);

        for (int ii = 0; ii < units.length; ii++) {
            UnitIcon icon = new UnitIcon(_ctx, units[ii]);
            // if they have a big shot for this icon, switch it
            BigShotItem bsitem = getBigShot(user, units[ii].type);
            if (bsitem != null) {
                icon.setItem(bsitem.getItemId(), bsitem.getName());
            } else {
                icon.setLocked(_ctx, true);
            }
            addIcon(icon);
        }
    }

    /**
     * Configures the palette to display the supplied user's Big Shots.
     *
     * @param filterTown if true, filter out Big Shots that are not available
     * in the current town.
     */
    public void setUser (PlayerObject user, boolean filterTown)
    {
        // listen to the user object for inventory additions
        _user = user;
        _user.addListener(_invlistener);

        // add icons for all existing big shots
        for (Item item : user.inventory) {
            if (item instanceof BigShotItem) {
                BigShotItem bsitem = (BigShotItem)item;
                UnitConfig config =
                    UnitConfig.getConfig(bsitem.getType(), false);
                if (config == null) {
                    // we may be seeing our ITP bigshots on a client that has
                    // never been to ITP... no problem, just skip 'em
                    continue;
                }
                if (filterTown && BangUtil.getTownIndex(user.townId) <
                    BangUtil.getTownIndex(config.getTownId())) {
                    continue;
                }
                UnitIcon icon = new UnitIcon(_ctx, config);
                icon.setItem(bsitem.getItemId(), bsitem.getName());
                addIcon(icon);
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
     * This must be called when the palette is going away to allow it to remove
     * its listener registrations.
     */
    public void shutdown ()
    {
        // remove our listener if we've configured one
        if (_user != null) {
            _user.removeListener(_invlistener);
            _user = null;
        }
    }

    protected BigShotItem getBigShot (PlayerObject user, String type)
    {
        for (Item item : user.inventory) {
            if (item instanceof BigShotItem &&
                ((BigShotItem)item).getType().equals(type)) {
                return (BigShotItem)item;
            }
        }
        return null;
    }

    protected void updateIcon (BigShotItem unit)
    {
        for (SelectableIcon sicon : _icons) {
            UnitIcon icon = (UnitIcon)sicon;
            if (icon.getUnit().type.equals(unit.getType())) {
                icon.setItem(unit.getItemId(), unit.getName());
                // trigger our inspector to refresh if needed
                if (_inspector != null) {
                    _inspector.iconUpdated(icon, icon.isSelected());
                }
            }
        }
    }

    protected SetAdapter<Item> _invlistener = new SetAdapter<Item>() {
        public void entryAdded (EntryAddedEvent<Item> event) {
            if (event.getName().equals(PlayerObject.INVENTORY)) {
                Object item = event.getEntry();
                if (item instanceof BigShotItem) {
                    updateIcon((BigShotItem)item);
                }
            }
        }
    };

    protected BangContext _ctx;
    protected PlayerObject _user;
}
