//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.event;

/**
 * Used to dispatch notifications of text changes in text components.
 */
public class TextEvent extends BEvent
{
    public TextEvent (Object source, long when)
    {
        super(source, when);
    }

    // documentation inherited
    public void dispatch (ComponentListener listener)
    {
        super.dispatch(listener);

        if (listener instanceof TextListener) {
            ((TextListener)listener).textChanged(this);
        }
    }

    // documentation inherited
    public boolean propagateUpHierarchy ()
    {
        return false;
    }
}
