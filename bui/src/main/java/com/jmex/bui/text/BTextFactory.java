//
// BUI - a user interface library for the JME 3D engine
// Copyright (C) 2005-2006, Michael Bayne, All Rights Reserved
// https://code.google.com/p/jme-bui/

package com.jmex.bui.text;

import com.jme3.math.ColorRGBA;

import com.jmex.bui.BConstants;

/**
 * Creates instances of {@link BText} using a particular technology and a particular font
 * configuration.
 */
public abstract class BTextFactory
    implements BConstants
{
    /**
     * Supplies {@link BTextFactory} instances for a platform whose text stack is not the default
     * AWT/Graphics2D one. Registered via {@link #setProvider}; when set, callers that build text
     * factories (the stylesheet) delegate to it instead of instantiating {@code AWTTextFactory}.
     * This is the seam the Android port uses to render text with {@code android.graphics} instead
     * of the (Harmony/sfntly) AWT font peer, which does not work on ART. Unused on desktop.
     */
    public interface Provider
    {
        /**
         * Creates a text factory for the given font family and size. {@code style} uses the
         * {@code java.awt.Font} style bits ({@code PLAIN=0}, {@code BOLD=1}, {@code ITALIC=2},
         * {@code BOLD|ITALIC=3}) so callers need not depend on any platform font type.
         */
        BTextFactory createTextFactory (String family, int style, int size);
    }

    /** Installs a platform text-factory provider (see {@link Provider}); {@code null} to clear.
     *  Set once at startup before any widget lays out text. */
    public static void setProvider (Provider provider)
    {
        _provider = provider;
    }

    /** Returns the installed platform text-factory provider, or {@code null} (the default, meaning
     *  use the AWT text stack). */
    public static Provider getProvider ()
    {
        return _provider;
    }

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

    /** The installed platform text-factory provider (Android uses this); null on desktop. */
    protected static Provider _provider;
}
