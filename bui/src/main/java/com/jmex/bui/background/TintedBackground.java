//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.background;

import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;

import com.jmex.bui.BComponent;
import com.jmex.bui.backend.BackendProvider;

/**
 * Displays a partially transparent solid color in the background.
 */
public class TintedBackground extends BBackground
{
    /**
     * Creates a tinted background with the specified color.
     */
    public TintedBackground (ColorRGBA color)
    {
        _color = color;
    }

    // documentation inherited
    public void render (RenderManager renderer, int x, int y, int width, int height,
        float alpha)
    {
        super.render(renderer, x, y, width, height, alpha);

        BComponent.applyDefaultStates();
        BackendProvider.get().applyBlendState();

        BackendProvider.get().fillRect(x, y, width, height,
            _color.r, _color.g, _color.b, _color.a * alpha);
    }

    protected ColorRGBA _color;
}
