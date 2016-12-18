//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.event;

/**
 * Generated when a model is changed.
 */
public class ChangeEvent extends BEvent
{
    public ChangeEvent (Object source)
    {
        super(source, -1L);
    }

    // documentation inherited
    public boolean propagateUpHierarchy ()
    {
        return false;
    }
}
