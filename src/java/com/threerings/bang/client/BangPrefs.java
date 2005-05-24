//
// $Id$

package com.threerings.bang.client;

import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.Display;

import com.jme.system.PropertiesIO;

import com.samskivert.util.Config;

/**
 * Contains client-side preferences.
 */
public class BangPrefs
{
    /** Contains our client-side preferences. */
    public static Config config = new Config("bang");

    /**
     * Returns the display mode in the supplied list that matches the one
     * configured as our preference, or null if there are no matches.
     */
    public static void configureDisplayMode (PropertiesIO props)
    {
        DisplayMode mode = Display.getDisplayMode();
        String dwidth = String.valueOf(Math.min(1024, mode.getWidth()));
        props.set("WIDTH", config.getValue("display_width", dwidth));
        String dheight = String.valueOf(Math.min(768, mode.getHeight()));
        props.set("HEIGHT", config.getValue("display_height", dheight));
        String dbpp = String.valueOf(mode.getBitsPerPixel());
        props.set("DEPTH", config.getValue("display_bpp", dbpp));
        String dfreq = String.valueOf(mode.getFrequency());
        props.set("FREQ", config.getValue("display_freq", dfreq));
        props.set("FULLSCREEN", String.valueOf(isFullscreen()));
        props.set("RENDERER", "LWJGL");
    }

    /**
     * Returns whether or not we prefer fullscreen mode.
     */
    public static boolean isFullscreen ()
    {
        return config.getValue("display_fullscreen", false);
    }

    /**
     * Stores our preferred display mode.
     */
    public static void updateDisplayMode (DisplayMode mode)
    {
        config.setValue("display_width", mode.getWidth());
        config.setValue("display_height", mode.getHeight());
        config.setValue("display_bpp", mode.getBitsPerPixel());
        config.setValue("display_freq", mode.getFrequency());
    }

    /**
     * Updates our fullscreen preference.
     */
    public static void updateFullscreen (boolean fullscreen)
    {
        config.setValue("display_fullscreen", fullscreen);
    }
}
