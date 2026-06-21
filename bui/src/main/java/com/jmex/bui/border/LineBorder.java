//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.border;

import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;

import com.jmex.bui.BComponent;
import com.jmex.bui.backend.BackendProvider;
import com.jmex.bui.util.Insets;

/**
 * Defines a border that displays a single line around the bordered component in a specified color.
 */
public class LineBorder extends BBorder
{
    public LineBorder (ColorRGBA color)
    {
        this(color, 1);
    }

    public LineBorder (ColorRGBA color, int width)
    {
        _color = color;
        _width = width;
    }

    @Override // from BBorder
    public Insets adjustInsets (Insets insets)
    {
        return new Insets(_width + insets.left, _width + insets.top,
                          _width + insets.right, _width + insets.bottom);
    }

    @Override // from BBorder
    public void render (RenderManager renderer, int x, int y, int width, int height, float alpha)
    {
        super.render(renderer, x, y, width, height, alpha);

        BComponent.applyDefaultStates();
        BackendProvider.get().applyBlendState();

        BackendProvider.get().drawRectOutline(x, y, width, height, _width,
            _color.r, _color.g, _color.b, _color.a * alpha);
    }

    protected ColorRGBA _color;
    protected int _width;

    protected static final Insets ONE_PIXEL_INSETS = new Insets(1, 1, 1, 1);
}
