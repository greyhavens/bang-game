//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.icon;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.Icon;

import com.jme.renderer.Renderer;
import com.jmex.bui.BImage;

/**
 * Displays an image as an icon.
 */
public class ImageIcon extends BIcon
{
    /**
     * Creates an icon from the supplied source image.
     */
    public ImageIcon (BImage image)
    {
        _image = image;
    }

    /**
     * Converts the supplied AWT icon into a BUI icon.
     */
    public ImageIcon (Icon icon)
    {
        BufferedImage cached = new BufferedImage(
            icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D gfx = cached.createGraphics();
        try {
            icon.paintIcon(null, gfx, 0, 0);
            _image = new BImage(cached);
        } finally {
            gfx.dispose();
        }
    }

    // documentation inherited
    public int getWidth ()
    {
        return _image.getWidth();
    }

    // documentation inherited
    public int getHeight ()
    {
        return _image.getHeight();
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
        _image.render(renderer, x, y, alpha);
    }

    protected BImage _image;
}
