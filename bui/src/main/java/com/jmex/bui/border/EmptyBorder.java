//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.border;

import com.jmex.bui.util.Insets;

/**
 * Defines a border with no rendered geometry but that simply takes up
 * space.
 */
public class EmptyBorder extends BBorder
{
    public EmptyBorder (int left, int top, int right, int bottom)
    {
        _insets = new Insets(left, top, right, bottom);
    }

    // documentation inherited
    public Insets adjustInsets (Insets insets)
    {
        return _insets.add(insets);
    }

    protected Insets _insets;
}
