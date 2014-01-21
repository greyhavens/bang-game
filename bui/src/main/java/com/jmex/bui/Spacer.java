//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui;

import com.jmex.bui.util.Dimension;

/**
 * Takes up space!
 */
public class Spacer extends BComponent
{
    /**
     * Creates a 1x1 spacer that will presumably be later resized by a layout
     * manager in some appropriate manner.
     */
    public Spacer ()
    {
        this(1, 1);
    }

    /**
     * Creates a spacer with the specified preferred dimensions.
     */
    public Spacer (int prefWidth, int prefHeight)
    {
        setPreferredSize(new Dimension(prefWidth, prefHeight));
    }
}
