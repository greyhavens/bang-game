//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.icon;

import com.jme.renderer.Renderer;

/**
 * Takes up space.
 */
public class BlankIcon extends BIcon
{
    public BlankIcon (int width, int height)
    {
        _width = width;
        _height = height;
    }

    // documentation inherited
    public int getWidth ()
    {
        return _width;
    }

    // documentation inherited
    public int getHeight ()
    {
        return _height;
    }

    // documentation inherited
    public void render (Renderer renderer, int x, int y, float alpha)
    {
        // nothing doing
    }

    protected int _width, _height;
}
