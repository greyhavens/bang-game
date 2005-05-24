//
// $Id$

package com.threerings.bang.client;

import java.util.logging.Level;

import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.Display;

import com.jme.system.PropertiesIO;

import com.samskivert.util.Config;

import static com.threerings.bang.Log.log;

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
        DisplayMode mode = getClosest(1024, 768, 16, 60);
        if (mode == null) {
            mode = Display.getDisplayMode();
        }
        String dwidth = String.valueOf(mode.getWidth());
        props.set("WIDTH", config.getValue("display_width", dwidth));
        String dheight = String.valueOf(mode.getHeight());
        props.set("HEIGHT", config.getValue("display_height", dheight));
        String dbpp = String.valueOf(mode.getBitsPerPixel());
        props.set("DEPTH", config.getValue("display_bpp", dbpp));
        String dfreq = String.valueOf(mode.getFrequency());
        props.set("FREQ", config.getValue("display_freq", dfreq));
        props.set("FULLSCREEN", String.valueOf(isFullscreen()));
        props.set("RENDERER", "LWJGL");
        log.info("Display mode: " + props.getWidth() + "x" + props.getHeight() +
                 "x" + props.getDepth() + " " + props.getFreq() + "Hz " +
                 "(current: " + mode + ").");
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

    /**
     * Returns the closest display mode to our specified default.
     */
    protected static DisplayMode getClosest (
        int width, int height, int depth, int freq)
    {
        DisplayMode c = null;
        try {
            DisplayMode[] modes = Display.getAvailableDisplayModes();
            for (int ii = 0; ii < modes.length; ii++) {
                DisplayMode mode = modes[ii];
                // apparently LWJGL can't cope with >24 bpp
                if (mode.getBitsPerPixel() > 24) {
                    continue;
                }
                if (c == null) {
                    c = mode;
                } else if (closer(c.getWidth(), mode.getWidth(), width)) {
                    c = mode;
                } else if (closer(c.getHeight(), mode.getHeight(), height)) {
                    c = mode;
                } else if (closer(c.getBitsPerPixel(), mode.getBitsPerPixel(),
                                  depth)) {
                    c = mode;
                } else if (closer(c.getFrequency(), mode.getFrequency(), freq)) {
                    c = mode;
                }
            }
            return c;

        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to enumerate display modes.", e);
            return null;
        }
    }

    protected static boolean closer (int value, int ovalue, int tvalue)
    {
        return Math.abs(value-tvalue) > Math.abs(ovalue-tvalue);
    }
}
