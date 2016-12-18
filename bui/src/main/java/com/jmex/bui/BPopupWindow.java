//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import com.jme.system.DisplaySystem;

import com.jmex.bui.layout.BLayoutManager;

/**
 * A window that is popped up to display something like a menu or a
 * tooltip or some other temporary, modal overlaid display.
 */
public class BPopupWindow extends BWindow
{
    public BPopupWindow (BWindow parent, BLayoutManager layout)
    {
        super(parent.getStyleSheet(), layout);
        _parentWindow = parent;
        setLayer(parent.getLayer());

        // set up our background and border from the look and feel
//         setBackground(_lnf.createPopupBackground());
//         setBorder(_lnf.createPopupBorder());
    }

    @Override // documentation inherited
    public boolean shouldShadeBehind ()
    {
        return false;
    }

    /**
     * Sizes the window to its preferred size and then displays it at the
     * specified coordinates extending either above the location or below
     * as specified. The window position may be adjusted if it does not
     * fit on the screen at the specified coordinates.
     */
    public void popup (int x, int y, boolean above)
    {
        // add ourselves to the interface hierarchy if we're not already
        if (_root == null) {
            _parentWindow.getRootNode().addWindow(this);
        }

        // size and position ourselves appropriately
        packAndFit(x, y, above);
    }

    /**
     * Called after we have been added to the display heirarchy to pack and
     * position this popup window.
     */
    protected void packAndFit (int x, int y, boolean above)
    {
        pack();

        // adjust x and y to ensure that we fit on the screen
        int width = DisplaySystem.getDisplaySystem().getWidth();
        int height = DisplaySystem.getDisplaySystem().getHeight();
        x = Math.min(width - getWidth(), x);
        y = above ?
            Math.min(height - getHeight(), y) : Math.max(0, y - getHeight());
        setLocation(x, y);
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "popupwindow";
    }
}
