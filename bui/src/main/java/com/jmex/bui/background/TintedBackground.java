//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.background;

import org.lwjgl.opengl.GL11;

import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;

import com.jmex.bui.BComponent;
import com.jmex.bui.BImage;

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
    public void render (Renderer renderer, int x, int y, int width, int height,
        float alpha)
    {
        super.render(renderer, x, y, width, height, alpha);

        BComponent.applyDefaultStates();
        BImage.blendState.apply();

        GL11.glColor4f(_color.r, _color.g, _color.b, _color.a * alpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
    }

    protected ColorRGBA _color;
}
