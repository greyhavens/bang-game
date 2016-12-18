//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.background;

import com.jme.renderer.Renderer;

/**
 * Provides additional information about a background that is used to display
 * the backgrounds of various components.
 */
public abstract class BBackground
{
    /**
     * Returns the minimum width allowed by this background.
     */
    public int getMinimumWidth ()
    {
        return 1;
    }

    /**
     * Returns the minimum height allowed by this background.
     */
    public int getMinimumHeight ()
    {
        return 1;
    }

    /** Renders this background. */
    public void render (Renderer renderer, int x, int y, int width, int height,
        float alpha)
    {
    }

    /**
     * Called when the component that contains this background is was added to
     * the interface hierarchy.
     */
    public void wasAdded ()
    {
    }

    /**
     * Called when the component that contains this background is no longer
     * part of a user interface hierarchy.
     */
    public void wasRemoved ()
    {
    }
}
