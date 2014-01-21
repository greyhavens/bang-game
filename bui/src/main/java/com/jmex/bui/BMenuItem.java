//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.BEvent;
import com.jmex.bui.event.MouseEvent;
import com.jmex.bui.icon.BIcon;

/**
 * Displays a single menu item.
 */
public class BMenuItem extends BLabel
{
    /**
     * Creates a menu item with the specified text that will generate an
     * {@link ActionEvent} with the specified action when selected.
     */
    public BMenuItem (String text, String action)
    {
        this(text, null, action);
    }

    /**
     * Creates a menu item with the specified icon that will generate an
     * {@link ActionEvent} with the specified action when selected.
     */
    public BMenuItem (BIcon icon, String action)
    {
        this(null, icon, action);
    }

    /**
     * Creates a menu item with the specified text and icon that will generate
     * an {@link ActionEvent} with the specified action when selected.
     */
    public BMenuItem (String text, BIcon icon, String action)
    {
        super(text);
        if (icon != null) {
            setIcon(icon);
        }
        _action = action;
    }

    /**
     * Returns the action configured for this menu item.
     */
    public String getAction ()
    {
        return _action;
    }

    // documentation inherited
    public boolean dispatchEvent (BEvent event)
    {
        if (event instanceof MouseEvent) {
            MouseEvent mev = (MouseEvent)event;
            switch (mev.getType()) {
            case MouseEvent.MOUSE_ENTERED:
                _armed = _pressed;
                break; // we don't consume this event

            case MouseEvent.MOUSE_EXITED:
                _armed = false;
                break; // we don't consume this event

            case MouseEvent.MOUSE_PRESSED:
                if (mev.getButton() == 0) {
                    _pressed = true;
                    _armed = true;
                } else if (mev.getButton() == 1) {
                    // clicking the right mouse button after arming the
                    // component disarms it
                    _armed = false;
                }
                return true; // consume this event

            case MouseEvent.MOUSE_RELEASED:
                if (_armed && _pressed) {
                    // create and dispatch an action event
                    fireAction(mev.getWhen(), mev.getModifiers());
                    _armed = false;
                }
                _pressed = false;
                return true; // consume this event
            }
        }

        return super.dispatchEvent(event);
    }

    // documentation inherited
    protected String getDefaultStyleClass ()
    {
        return "menuitem";
    }

    /**
     * Called when the menu item is "clicked" which may due to the mouse
     * being pressed and released while over the item or due to keyboard
     * manipulation while the item has focus.
     */
    protected void fireAction (long when, int modifiers)
    {
        if (_parent instanceof BPopupMenu) {
            ((BPopupMenu)_parent).itemSelected(this, when, modifiers);
        }
    }

    protected String _action;
    protected boolean _armed, _pressed;
}
