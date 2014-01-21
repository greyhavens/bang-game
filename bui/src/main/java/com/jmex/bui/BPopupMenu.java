//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import java.util.ArrayList;

import com.jme.system.DisplaySystem;

import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.layout.TableLayout;
import com.jmex.bui.util.Dimension;

/**
 * Displays a popup menu of items, one of which can be selected.
 */
public class BPopupMenu extends BPopupWindow
{
    public BPopupMenu (BWindow parent)
    {
        this(parent, false);
    }

    public BPopupMenu (BWindow parent, boolean horizontal)
    {
        super(parent, null);
        setLayoutManager(
                (horizontal ? GroupLayout.makeHStretch() : GroupLayout.makeVStretch()).setGap(0));
        _columns = 1;
        _modal = true;
    }

    public BPopupMenu (BWindow parent, int columns)
    {
        super(parent, null);
        setPreferredColumns(columns);
        _modal = true;
    }

    /**
     * Adds the supplied item to this menu.
     */
    public void addMenuItem (BMenuItem item)
    {
        // nothing more complicated needs to be done, yay!
        add(item, GroupLayout.FIXED);
    }

    /**
     * Sets the preferred number of columns.  Will relayout the existing menu items into the
     * preferred number of columns, but may add more columns if the menu will not fit in
     * the vertical space.
     */
    public void setPreferredColumns (int columns)
    {
        columns = Math.max(1, columns);
        if (columns == _columns) {
            return;
        }
        _columns = columns;
        ArrayList<BComponent> children = new ArrayList<BComponent>(_children);
        removeAll();
        if (_columns == 1) {
            setLayoutManager(GroupLayout.makeVStretch().setGap(0));
        } else {
            setLayoutManager(new TableLayout(_columns, 0, 5));
        }
        for (BComponent child : children) {
            add(child);
        }
    }

    // documentation inherited
    public boolean dispatchEvent (BEvent event)
    {
        if (event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            // if the mouse clicked outside of our window bounds, dismiss
            // ourselves
            if (mev.getType() == MouseEvent.MOUSE_PRESSED &&
                getHitComponent(mev.getX(), mev.getY()) == null) {
                dismiss();
                return true;
            }
        }
        return super.dispatchEvent(event);
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "popupmenu";
    }

    // documentation inherited
    protected void packAndFit (int x, int y, boolean above)
    {
        int width = DisplaySystem.getDisplaySystem().getWidth();
        int height = DisplaySystem.getDisplaySystem().getHeight();

        // determine whether we can fit in the window
        ArrayList<BComponent> children = null;
        int columns = _columns;
        do {
            Dimension d = getPreferredSize(-1, -1);
            if (d.height > height) {
                // remove our children, switch to a table layout and readd
                if (children == null) {
                    children = new ArrayList<BComponent>(_children);
                }
                removeAll();
                setLayoutManager(new TableLayout(++columns, 0, 5));
                for (int ii = 0; ii < children.size(); ii++) {
                    add(children.get(ii));
                }
            } else {
                break;
            }
        } while (columns < 4);

        // now actually lay ourselves out
        pack();

        // adjust x and y to ensure that we fit on the screen
        x = Math.min(width - getWidth(), x);
        y = above ?
            Math.min(height - getHeight(), y) : Math.max(0, y - getHeight());
        setLocation(x, y);
    }

    /**
     * Called by any child {@link BMenuItem}s when they are selected.
     */
    protected void itemSelected (BMenuItem item, long when, int modifiers)
    {
        emitEvent(new ActionEvent(item, when, modifiers, item.getAction()));
        dismiss();
    }

    protected int _columns;
}
