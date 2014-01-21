//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.border;

import com.jme.renderer.Renderer;
import com.jmex.bui.util.Insets;

/**
 * Configures a border around a component that may or may not have
 * associated geometric elements. <em>Note:</em> a border must only be
 * used with a single component at a time.
 */
public abstract class BBorder
{
    /**
     * Adds the supplied insets to this border's insets and returns adjusted
     * insets.
     */
    public abstract Insets adjustInsets (Insets insets);

    /** Renders this border. */
    public void render (Renderer renderer, int x, int y, int width, int height,
        float alpha)
    {
    }

    /**
     * Called when the component that contains this border is was added to the
     * interface hierarchy.
     */
    public void wasAdded ()
    {
    }

    /**
     * Called when the component that contains this border is no longer part of
     * a user interface hierarchy.
     */
    public void wasRemoved ()
    {
    }
}
