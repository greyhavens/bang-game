//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.icon;

import com.jme.renderer.Renderer;

import com.jmex.bui.BImage;
import com.jmex.bui.util.Rectangle;

/**
 * Displays a region of an image as an icon.
 */
public class SubimageIcon extends BIcon
{
    /**
     * Creates an icon that will display the specified region of the supplied
     * image.
     */
    public SubimageIcon (BImage image, int x, int y, int width, int height)
    {
        _region = new Rectangle(x, y, width, height);
        _image = image;
    }

    // documentation inherited
    public int getWidth ()
    {
        return _region.width;
    }

    // documentation inherited
    public int getHeight ()
    {
        return _region.height;
    }

    // documentation inherited
    public void wasAdded ()
    {
        super.wasAdded();
        _image.reference();
    }

    // documentation inherited
    public void wasRemoved ()
    {
        super.wasRemoved();
        _image.release();
    }

    // documentation inherited
    public void render (Renderer renderer, int x, int y, float alpha)
    {
        super.render(renderer, x, y, alpha);
        _image.render(renderer, _region.x, _region.y,
                      _region.width, _region.height, x, y, alpha);
    }

    protected BImage _image;
    protected Rectangle _region;
}
