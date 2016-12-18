//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.text;

import com.jme.renderer.Renderer;
import com.jmex.bui.util.Dimension;

/**
 * Contains a "run" of text.  Specializations of this class render text in different ways, for
 * example using JME's internal bitmapped font support or by using the AWT to render the run of
 * text to an image and texturing a quad with that entire image.
 */
public abstract class BText
{
    /**
     * Returns the length in characters of this text.
     */
    public abstract int getLength ();

    /**
     * Returns the screen dimensions of this text.
     */
    public abstract Dimension getSize ();

    /**
     * Returns the character position to which the cursor should be moved given that the user
     * clicked the specified coordinate (relative to the text's bounds).
     */
    public abstract int getHitPos (int x, int y);

    /**
     * Returns the x position for the cursor at the specified character index. Note that the
     * position should be "before" that character.
     */
    public abstract int getCursorPos (int index);

    /**
     * Renders this text to the display.
     */
    public abstract void render (Renderer render, int x, int y, float alpha);

    /**
     * Optional rendering this text scaled to a certain height/width.
     */
    public void render (Renderer render, int x, int y, int w, int h, float alpha)
    {
        render(render, x, y, alpha);
    }

    /**
     * Called when the component that contains this text is was added to the interface hierarchy.
     */
    public abstract void wasAdded ();

    /**
     * Called when the component that contains this text is no longer part of a user interface
     * hierarchy.
     */
    public abstract void wasRemoved ();
}
