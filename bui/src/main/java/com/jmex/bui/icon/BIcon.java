//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.icon;

import com.jme.renderer.Renderer;

/**
 * Provides icon imagery for various components which make use of it.
 */
public abstract class BIcon
{
    /** Returns the width of this icon. */
    public abstract int getWidth ();

    /** Returns the height of this icon. */
    public abstract int getHeight ();

    /** Renders this icon. */
    public void render (Renderer renderer, int x, int y, float alpha)
    {
    }

    /**
     * Called when the component that contains this icon is was added to the
     * interface hierarchy.
     */
    public void wasAdded ()
    {
    }

    /**
     * Called when the component that contains this icon is no longer part of a
     * user interface hierarchy.
     */
    public void wasRemoved ()
    {
    }
}
