//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.border;

import com.jmex.bui.util.Insets;
import com.jme.renderer.Renderer;

/**
 * Combines two borders into a single compound border.
 */
public class CompoundBorder extends BBorder
{
    public CompoundBorder (BBorder outer, BBorder inner)
    {
        _outer = outer;
        _inner = inner;
        _insets = outer.adjustInsets(Insets.ZERO_INSETS);
    }

    // documentation inherited
    public Insets adjustInsets (Insets insets)
    {
        return _outer.adjustInsets(_inner.adjustInsets(insets));
    }

    // documentation inherited
    public void render (Renderer renderer, int x, int y, int width, int height,
        float alpha)
    {
        _outer.render(renderer, x, y, width, height, alpha);
        _inner.render(renderer, x + _insets.left, y + _insets.bottom,
                      width - _insets.getHorizontal(),
                      height - _insets.getVertical(), alpha);
    }

    protected BBorder _outer, _inner;
    protected Insets _insets;
}
