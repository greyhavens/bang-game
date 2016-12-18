//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.event;

/**
 * An event dispatched to a component when it receives or loses the focus.
 */
public class FocusEvent extends BEvent
{
    /** Indicates that a component gained the focus. */
    public static final int FOCUS_GAINED = 0;

    /** Indicates that a component lost the focus. */
    public static final int FOCUS_LOST = 1;

    public FocusEvent (Object source, long when, int type)
    {
        super(source, when);
        _type = type;
    }

    /**
     * Indicates whether this was a {@link #FOCUS_GAINED} or {@link
     * #FOCUS_LOST} event.
     */
    public int getType ()
    {
        return _type;
    }

    // documentation inherited
    public boolean propagateUpHierarchy ()
    {
        return false;
    }

    protected int _type;
}
