//
// $Id$

package com.threerings.bang.ranch.client;

import com.jme.bui.BContainer;
import com.jme.bui.border.EmptyBorder;
import com.jme.bui.layout.TableLayout;

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
        _inspector.setUnit(icon.getUnit());
    }

    protected BangContext _ctx;
    protected UnitInspector _inspector;
}
