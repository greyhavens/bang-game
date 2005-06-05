//
// $Id$

package com.threerings.bang.ranch.client;

import com.jme.bui.BContainer;
import com.jme.bui.layout.TableLayout;

import com.threerings.bang.data.BigShot;
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
        _ctx = ctx;
        _inspector = inspector;
    }

    /**
     * Configures the palette to display the specified bigshot units.
     */
    public void setBigShots (BigShot[] units)
    {
        for (int ii = 0; ii < units.length; ii++) {
            add(new UnitIcon(_ctx, units[ii].getType()));
        }
    }

    /**
     * Configures the palette to display the specified normal units.
     */
    public void setUnits (String[] units)
    {
        for (int ii = 0; ii < units.length; ii++) {
            add(new UnitIcon(_ctx, units[ii]));
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
        _inspector.setUnit(icon.getType());
    }

    protected BangContext _ctx;
    protected UnitInspector _inspector;
}
