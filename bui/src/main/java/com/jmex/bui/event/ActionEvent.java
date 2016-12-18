//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.event;

/**
 * Dispatched by a component when some sort of component-specific action
 * has occurred.
 */
public class ActionEvent extends InputEvent
{
    public ActionEvent (Object source, long when, int modifiers, String action)
    {
        super(source, when, modifiers);
        _action = action;
    }

    /**
     * Returns the action associated with this event.
     */
    public String getAction ()
    {
        return _action;
    }

    // documentation inherited
    public void dispatch (ComponentListener listener)
    {
        super.dispatch(listener);
        if (listener instanceof ActionListener) {
            ((ActionListener)listener).actionPerformed(this);
        }
    }

    // documentation inherited
    public boolean propagateUpHierarchy ()
    {
        return false;
    }

    protected void toString (StringBuffer buf)
    {
        super.toString(buf);
        buf.append(", action=").append(_action);
    }

    protected String _action;
}
