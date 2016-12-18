//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.text;

import com.jme.renderer.ColorRGBA;

import com.jmex.bui.BConstants;

/**
 * Creates instances of {@link BText} using a particular technology and a particular font
 * configuration.
 */
public abstract class BTextFactory
    implements BConstants
{
    /**
     * Returns the height of our text.
     */
    public abstract int getHeight ();

    /**
     * Creates a text instance using our the font configuration associated with this text factory
     * and the foreground color specified.
     */
    public BText createText (String text, ColorRGBA color)
    {
        return createText(text, color, NORMAL, DEFAULT_SIZE, null, false);
    }

    /**
     * Creates a text instance using our the font configuration associated with this text factory
     * and the foreground color, text effect and text effect color specified.
     *
     * @param useAdvance if true, the advance to the next insertion point will be included in the
     * bounds of the created text (this is needed by editable text displays).
     */
    public abstract BText createText (String text, ColorRGBA color, int effect, int effectSize,
                                      ColorRGBA effectColor, boolean useAdvance);

    /**
     * Wraps a string into a set of text objects that do not exceed the specified width.
     */
    public abstract BText[] wrapText (String text, ColorRGBA color, int effect, int effectSize,
                                      ColorRGBA effectColor, int maxWidth);
}
